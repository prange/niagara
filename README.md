# niagara
A very lighweight and thin streaming library, perfect for EventDriven applications.


Okay, but does this not already exist?

#### java.util.Stream
Ultra fast, no dependency, parallell processing support and even resource cleanup, albeit
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
is hogs resources. The basic api is beatiful, and they can make strong guarantess that all
resoruces are cleaned up in the correct order. Its is modeled as a state machine, so its "impossible"
to put the stream in an illegal state. This is my got for medium length streams, for example
processing large files, because just uses the minimum rquired resources. But since it is a state-
machine there is a high object churn.


#### Kembe
The predecessor of this project. It was in turn inspired by _kefir.js_. A very lightweight
streaming implementation for js. Kembe was thin, but lacked some resource control and coordination, but it 
yielded very little overhead.

Lets see if we can combine the knowledge from these libs to make something that strikes
the - or, at least our - sweetspot regarding our requirement a little better.


## What does "Event Driven" mean anyway?

In an event-driven application (EDA) input and output to the application is modelled as events.
Events are first order citicens of the domain language used in the application. An EDA is inherently
asynchrous, and is easy to split up and partition since internal comunication also is modelled as events.
A very nice feture of events is that they can be queued and sent over the network. Async communication

When modelling information flow as events som very nice traits emerge. 
 * Input can be modelled as a stream.
 * Input can be viewed over time.
 * State in the application can be modelled as a left fold (if this seems weird, or it this sounds like som papercut thing, keep reading...)



## Abstraction
A good abstraction helps understanding how a library is meant to work, ot provides guidelines for
the library designers. Instead of _stream of values_ or _observables and observers_ we have opted for
_source of messages that make repeated calls on your handler_. Sounds weird, but make sense after a while.

Lets call this block of code a _Source_. A `Source<A>` pushes events of type a to some _listener_.

A `SourceListener<A>` listens for messages of type A and starts a (possibly async) computation. This computation 
is represented by the value `Eventually<Result>`. This means that when the `Source` pushes som 
message `A` to the `Listener`, the listener will _eventually_ yield a `Result`. The `Result` type
is just information about the state of the listener. It will never return some other useful value.

This makes perfect sense in an async world where alle communication is fire-and-forget. If you want answers,
you must make the app push messages back to you

Lets just start with an example and take it from there:

```java
public class Example1 {

    public static void main(String[] args) {

        Source<String> strings =
          Sources.values("one", "two", "three");

        strings
          .apply(Example1::println)
          .flatten(l-> Arrays.asList(l.split("")))
          .onClose(println("Closing flatten").toUnit())
          .apply(Example1::println)
          .run();
    }

    static Task<String> println(String line) {
        return Task.call(() -> {
            System.out.println(line);
            return line;
        });
    }

}

```
Lets start at the bottom: That _println_ method models printing to the console
as a task. What is a task? A task represents some (possibly async) computation or
IO - what we call sideeffects. Printing to the console is a mild sideeffect and by many considered so trivial that
modelling that as a Task is useless. That is up to you to decide, but here we use it to
simulate _any_ IO, represented as a println.

Then we look at the source: We see that it is made from three Strings. Later vi will
look at near unlimited sources (like databases) and really unlimited sources (like 
rest endpoints)

After the source is constructed we define our operations on the source.
For each value from the source we want to print it to console using _apply()_. Then we 
split the strings and send every letter individually to the next handler.
We can use _onClose_ for resource management, but here we just print that the stream was closed.

Then we want to print out the letters.

And finally we create a Task that can run the stream. Why all this indirection you think?
Its because we want to be able to preciely define when our stream starts. And it starts
exactly when we call _execute()_ on the Task.



This was boring. Printing to console is easy, no need for streaming to do that...
Lets start to build up by createing a fake database source:

```java
public class SyncFakeDb implements Source<String> {

    private Random r = new Random();

    @Override
    public Eventually<Source.Running> open(SourceListener<String> f) {
        System.out.println("Faking open database");

        Stream.range(0, 10000000).foreachDoEffect(n -> {
            String randString = n + " " + r.nextLong();

            //We imperatively push events to the handler, awaiting the result
            //every time so that dont flood any doenstream handler.
            f.handle(randString).await(Duration.ofSeconds(2));

        });

        System.out.println("Faking close database");

        //Close and return after we are done. An async implementation must return at once,
        //and run this on another thread.
        return Eventually.value(new Source.Running(Eventually.value(Source.Result.ack)));
    }
}
```

As the name implies: its a synchronous implementation, which means that whatever thread
is used to run the stream task, is used to push the values from the database.

Api


#### onClose


#### append


#### bind (flatMap)


#### map


#### mapState
Like a continous fold that yields the computed state


#### mapMealy
Maps over a mealymachine (A state machine - really useful, google it!) and yields the output of the machine
Keeps the machine as state.


#### join


#### apply


#### flatten


#### mapState


#### run
