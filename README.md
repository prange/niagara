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
exactly when we call _execute()_ on the Task <sup>*</sup>.

<sup>*) Not really exactly. The source is actually signalled to start exactly when
you call start, but it might actually start at some later time, in a thread far far away.</sup>

## Constructing sources - part 1

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
is used to run the stream task, is used to push the values from the database. But that is
okay for now.

Lets see how this can be used:

Lets start by counting the entries (The "Hello world" of streaming applications)

```java
public class Example2 {
    
    public static void main(String[] args) {

        Source<Long> dbSource =
          new SyncFakeDb()
            .foldLeft(0L, (count, str) -> count + 1);


        dbSource.toTask().execute();

    }
}
```
### Introducing state

But wait - something is missing. Example2 is useless, it has no effects! You see, for streams
that are almost infinite, it makes no sense to return something. Its not like you want to 
keep the last request that arrives to a webserver. But storing data for later retrieval is a
common usecase, lets use AtomicLong for a counter.

```java
public class Example3 {

    static Task<Unit> set(AtomicLong atomicLong, long value) {
        return Task.runnableTask(() -> atomicLong.set(value));
    }

    static Task<Long> read(AtomicLong atomicLong) {
        return Task.call(atomicLong::get);
    }

    static Task<String> println(String line) {
        return Task.call(() -> {
            System.out.println(line);
            return line;
        });
    }
    
    public static void main(String[] args) {

        AtomicLong counter = new AtomicLong();

        //Constructing a source that reads from a db,
        //folds over its values and sets a counter
        Source<?> dbSource =
          new SyncFakeDb()
            .foldLeft(0L, (count, str) -> count + 1)
            .apply(sum -> set(counter, sum));

        //Creates a task that reads from the counter, and then prints
        //the result
        Task<String> print =
          read(counter)
            .flatMap(sum -> println("The sum is " + sum));

        //Starts the source and waits for completion
        dbSource.toTask().execute();

        //Prints out the value of the counter
        print.execute();
    }
}
```

Why all the `Task`s you ask? Well, reading and writing to the counter is not _referentially transparent_.
It means that _multiple calls to get() or set() might not yield the same result_.
The value _get()_ returns changes every time the counter is changed (obvously). But we dont
like that. We like that all functions return the same value every time we make
the same call. But we really need state, so we model it as a Task, and make it really clear
that when you execute this task, the world might change, and the result of the
task might be different every time. It makes it abolutely clear that we are working
with something that might change, or performs IO, or takes a long time, or might fail.

Tasks can be connected (_bound_) together to form more complicated tasks. All ran in order,
and aborted if something fails. But i digress - back to the example.

If you run the example you get
```
Faking open database
Faking close database
The sum is 1000000
```
as expected. 

### Introducing time

Time is something that we often ignore when writing programs. When writing simple
crud interfaces we dont need to think about time, we just receive a request, and update
the state in the store, or retrieve the state without much attention to when we retrieve it.

The problem arises when we have values that _change over time_. Changing values are hard.
A variable has a value depending on _when_ you access it. 

Example3 was single threaded, so time was really not an issue here. But would it be
if we introduced concurrency?

```java
public class Example4 {

    static Task<Unit> set(AtomicLong atomicLong, long value) {
        return Task.runnableTask(() -> atomicLong.set(value));
    }

    static Task<Long> read(AtomicLong atomicLong) {
        return Task.call(atomicLong::get);
    }

    static Task<String> println(String line) {
        return Task.call(() -> {
            System.out.println(line);
            return line;
        });
    }

    static final ExecutorService pool =
      Executors.newFixedThreadPool(2);

    public static void main(String[] args) {

        AtomicLong counter = new AtomicLong();

        //Creates a task that reads from the counter, and then prints
        //the result
        Task<String> print =
          read(counter)
            .flatMap(sum -> println("The sum is " + sum));

        //Constructing a source that reads from a db,
        //folds over its values and sets a counter
        Source<?> dbSource =
          new AsyncFakeDb(pool)
            .foldLeft(0L, (count, str) -> count + 1)
            .apply(sum -> set(counter, sum))
            .onClose(print); //Execute the effect when the stream is done
        
        //Starts the source and waits for completion
        dbSource.toTask().execute();

        //Execute the effect right away
        print.execute();
    }
}
```

This prints:
```
Faking open database
The sum is 0
Faking close database
The sum is 1000000
```

First the "database" is opened. That's obvious. But then our state is printed right away, before the
stream has even started to count. That is because the "database" is async. The _main_ thread reached the
_print.execute()_ before the pool started to produce values. We used _onClose_ to execute a task
when the stream ended. Since we cant know exaclty when it ends, we can ask it
to execute the task when it does. 

If you look at the code in the main method, you can begin to see how we use the
stream api as a dsl for orchestrating our application. We are starting to abstract away
one hard part, namely time by encapsuling it "inside" the stream or tasks. But we have
just begun scratching the surface, lets continue to play around.

We could append the async and the sync streams, making one start when the other stops:
(The tasks are factored out into the Utils class)

 ```java
public class Example5 {

    static final ExecutorService pool =
      Executors.newFixedThreadPool(2);

    public static void main(String[] args) {

        AtomicLong counter = new AtomicLong();

        Task<String> print =
          read(counter)
            .flatMap(sum -> println("The sum is " + sum));

        Source<?> dbSource =
          new AsyncFakeDb(pool)
            .onClose(println("Async closed").toUnit())
            .append(SyncFakeDb::new) //Append
            .foldLeft(0L, (count, str) -> count + 1)
            .apply(sum -> set(counter, sum))
            .onClose(print);

        dbSource.toTask().execute();
    }
}
```

which prints
```
Faking open async database
Faking close async database
Async closed
Faking open sync database
Faking close sync database
The sum is 2000000
```
Sources can be _map_ ped: 

```java
    Source<String> dbAsString =
          new AsyncFakeDb(pool).map(l->String.valueOf(l));
```
Which means its content can be changed value for value.

Sources can be _join_ ed:
```java
    Source<Long> dbs =
          new AsyncFakeDb(pool).join(new AsyncFakeDb(pool))
```

_Or_ ed:

```java
    Source<Either<Long,String>> counterOrString =
          new AsyncFakeDb(pool).or(strings1)
```

You can even make stream of streams using _bind_ (aka. _flatMap_)

```java
    Source<String> letters =
          new AsyncFakeDb(pool).bind(l->Streams.values(String.valueOf(l).split("")))
```


## Constructing sources - part 2
Up to now we have constructed streams using the Streams convenience methods or
pulling values from a source (the database) and pushing them to the handler.
This pattern doesnt work for cases where messages a pushed to us, for example 
when serving a jax-rs endpoint.

### Topics
That is what we have topics for. Topics are, well, topics. As in pub/sub topics.
You post something to a topic, and subscribers that subscribe to that topic
get what you post. Her those subscriptions are sources. When you publish a message to 
the a topic, all connected sources will be updated at once, and as concurrently as
your topology allows. The Task you receive when you publish will push the message when executed.
The task is resolved when all listeners have received the message. A topic never closes
and does not participate in resourcemanagement. (You will learn more about them later)

```java
public class Example7_topic {

    static final ExecutorService pool =
      Executors.newFixedThreadPool(1);

    public static void main(String[] args) {

        AtomicLong counter =
          new AtomicLong();

        AsyncTopic<String> topicA =
          new AsyncTopic<>(pool);

        Task<String> print =
          read(counter)
            .flatMap(sum -> println("The sum is " + sum));


        Source<String> dbSource =
          topicA
            .subscribe()
            .apply(Utils::println)
            .onClose(print);

        dbSource.toTask().execute();

        topicA.publish("First").execute();
        topicA.publish("Second").execute();
        topicA.publish("Third").execute().await(Duration.ofSeconds(10));
    }
}
```

### Queues
Queueus are the same as Topics, except messages you publish will wait for 
a consumer, and will always be async. The queue will make 
your app handle peaks where consumers have trouble in keeping up, and also
as a safetymeasure. The queue is bounded, and when the queue is full, messages are
dropped (oldest messages get dropped first). 

A queue never closes, and you can add to a queue even if no source is opened from the queue.

When you open a source from the queue, you get the messages that are currently in the queue.


```java
public class Example9_queue {

    static final ExecutorService pool =
      Executors.newFixedThreadPool(1);

    public static void main(String[] args) throws InterruptedException {


        AtomicLong counter =
          new AtomicLong();

        AsyncDroppingInputQueue<String> queue =
          new AsyncDroppingInputQueue<>(10, pool);

        long numberOfElements = 100;

        F<String, Task<Unit>> increment =
          u -> Task.runnableTask(counter::incrementAndGet);

        Task<Unit> print =
          read(counter)
            .flatMap(count ->
              println("The sum is " + count)).toUnit();

        Source<?> qSource =
          queue
            .subscribe()
            .apply(increment)
            .zipWithIndex()
            .asLongAs(tuple((index, value) -> index < numberOfElements))
            .onClose(print);

        //Open the source, so we get the values in the queue
        qSource.toTask().execute();

        Stream.range(0, numberOfElements).foreachDoEffect(n -> {
            queue.offer("Message " + n).execute().await(Duration.ofSeconds(1));
        });

    }
}
```

The keen eye will spot that the above program never will emit anything to the console,
because the stream probably will never close. The _asLongAs_ method will tell the
stream to continue as long as the index is lower than the max number of elements
that are produced. When the max is hit, the stream should close. But the max is never hit,
because messages are dropped from the queue, because the source cannot keep up with the
pace we are offering messages to the queue. We could make a queue that blocks the offering
thread while we are waiting for the source to catch up, but that would be deadlock prone. And in
the end, blocking should rarely be neccesary. Open a pull request or a ticket if you find a good usecase.
