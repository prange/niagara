# niagara
Lighweight, single threaded, really fast, perfect for EventDriven applications.




## What does "Event Driven" mean anyway?

In an event-driven application (EDA) input and output to the application is modelled as events.
Events are first order citicens of the domain language used in the application. An EDA is inherently
asynchrous, and is easy to split up and partition since internal comunication also is modelled as events.
A very nice feture of events is that they can be queued and sent over the network. 

When modelling information flow as events som very nice traits emerge. 
 * Input can be modelled as a stream.
 * Input can be viewed over time.
 * State in the application can be modelled as a left fold (if this seems weird, or it this sounds like som papercut thing, keep reading...)


## What? Single threaded?

_Yes._ And no... Running a stream in a single thread is the most performant 
and cleanest solution. No more memory that strictly neccecary is used, and no intermittent queueing of elements is done. 
There are however a couple multithreading options available. The most performant one is connecting streams in serial, letting one
thread handle one part of the workload at a time. Long running tasks can be split do multiple thread by an executorservice and the handed over
to a steam again, or the stream can be split between multiple threads in a deterministic fashion. Parallell stateful computations across threads is not
possible, and also not wanted. 

For really super high speed performance use the queues from the Agrona library.


## Show me the code

```java
package org.kantega.niagara.example;

import org.kantega.niagara.Eval;
import org.kantega.niagara.Plan;
import org.kantega.niagara.Plans;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class Example1 {

    public static void main(String[] args) {

        Plan<Integer> strings1 =
          Plans.emit(1, 2, 3);

        AtomicInteger counter = new AtomicInteger();


        strings1
          .map(n -> n + 1)
          .eval(Example1::println)
          .repeat()
          .takeWhile(n -> n > 5)
          .repeat()
          .flatMap(n -> Arrays.asList(n, n, n, n, n))
          .eval(Example1::println)
          .takeWhile(n -> counter.incrementAndGet() > 20)
          .build()
          .run();
    }

    static <A> Eval<A> println(A line) {
        return Eval.call(() -> {
            System.out.println(line);
            return line;
        });
    }

}
```


Okay, but does this not already exist?

#### java.util.Stream
Pretty fast, no dependency, parallell processing support and even resource cleanup, albeit
a bit clumsy. (Ever tries to close a java.util.Stream from the producer side, to make sure
cleanup is run on the consumer side?

#### reactive-streams (rxjava, akka-streams etc.)
Reactive streams is an API this is beeng adopted by the java sdk as the streaming data api.
Its abstraction are about `Observables` that produce values  and `Observers` that can observe them.
A popular abstraction which is easy to understand. It lacks somewhat in resource cleanup, and
and support for resourcelimitation is only thorugh backpressure which is handles a bit clumsily.
There is also plenty room for error when using the api, leading to illegal or useless states.
Rxjava is a popular implementation for java, Akka-streams is another one.

#### fs2
This is my favourite library, but it has a couple of drawbacks. It just available for scala, and
is not as performant as i would like. The basic api is beautiful, and they can make strong guarantess that all
resoruces are cleaned up in the correct order. Its is modeled as a state machine, so its "impossible"
to put the stream in an illegal state. This is my got for medium length streams, for example
processing large files, because just uses the minimum rquired resources. But since it is a state-
machine there is a high object churn. FS2 has been a huge inspiration for Niagara.




