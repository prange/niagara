# niagara
A very lighweight and thin streaming library. 


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




## Abstraction
A good abstraction helps understanding how a library is mant to work, ot provides guidelines for
the library designers. Instead of _stream of values_ or _observables and observers_ we have opted for
_blocks that make repeated calls_. Sounds weird, but make sense after a while.


We call the basic construct a block, and the definiton of a block is that it is some datastructure that
can make repeated calls to some handler.

``` java
public interface Block<A>{
    CompletableFuture<Unit> open(BlockHandler<A> f);
}

```

where a `BlockHandler<A>` essentially just is a function from A to Result. A block ha three states:

* Wating: When the block is created, before `open()` is called.
* Running: When open is called, the block immediately enters the `Running` state. Resources are aquired
.
* Stopped: When open is called, the running block yields a future that completes when the 
block enters stopped state. It will and shall forever not make any more calls. Resourcs are released.


It is noteworthy that all calls are syncronous. But streams (or blocks) are only useful when performing
effects, and one would expect asyncronizity from srtream libraries. That means that all blocks must handle
this internally, and always return all calls immediately. Long running taks like aquiring resurces
or reading from databases must be done asynchronously.
Look at the examples in the /test folder, it will clear things up.

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
