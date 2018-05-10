# WIP: niagara
Lightweight async programming for java, perfect for event-driven application. The library provides two constructs for
putting together reactive applications. Task and Plan.

A Task is similar to a Future, except that it does not actually do anything. Instead you provide it to a runtime which executes it. Fast. Faster than you.

A Plan is similar to a Stream, except that it does not actually do anything. Instead you provide it to a runtime and.. you get the idea.

But why?
Since Tasks and Plans are just values, you can pass them around as small programs in your application. They can be combined, parallized, run in order, canceled and so on. And when you have buildt your little program you can give it to a small runtime that optimizes it and runs it for you.




## What does "Event-Driven" mean anyway?

In an event-driven application (EDA) input and output to the application is modelled as events.
Events are first order citizens of the domain language used in the application. An EDA is inherently
asynchrous, and is easy to split up and partition since internal comunication also is modelled as events.
A very nice feture of events is that they can be queued and sent over the network. 

When modelling information flow as events som very nice traits emerge. 
 * Input can be modelled as a stream.
 * Input can be viewed over time.
 * State in the application can be modelled as a left fold (if this seems weird, or it this sounds like som papercut thing, keep reading...)



## Show me the code
A Task:
```java
public class TaskExample {

    public static void main(String[] args) {
        var unitTask =
          Console.prinln("One");

        var stringTask =
          value("string")
            .delay(Duration.ofSeconds(10));

        var integerTask =
          value(1234);

        var mapped =
          integerTask.map(String::valueOf);

        var joined =
          join(stringTask, mapped, (s1, s2) -> s1 + " " + s2);

        var printResult =
          joined.flatMap(Console::prinln);

        var rts = new TaskRuntime();
        rts.eval(fork(printResult, unitTask));
    }
}

```
A plan:
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
          .takeWhile(n -> n < 5)
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





