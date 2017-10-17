package org.kantega.niagara;

import fj.data.Option;
import fj.function.Effect1;
import fj.function.Try0;
import fj.function.TryEffect1;

import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

public class Sources {

    public static final Duration defaultTimeout = Duration.ofSeconds(10);


    public static <A> Source<A> nil() {
        return (closer, handler) -> Eventually.value(Source.ended());
    }

    public static <A> Source<A> value(A a) {
        return (closer, handler) ->
          handler.handle(a).execute().map(u -> Source.ended());
    }

    @SafeVarargs
    public static <A> Source<A> values(A... as) {
        return fromIterable(Arrays.asList(as));
    }

    public static <A> Source<A> fromIterable(Iterable<A> as) {
        return value(as).flatten(i -> i);
    }

    public static <A> Source<A> fromIterator(Iterator<A> as) {
        return callback(cb->as.forEachRemaining(cb::handle));
    }

    public static <A> Source<A> callback(Effect1<SourceListener<A>> callbackReceiver) {
        return (closer, handler) -> {
            callbackReceiver.f(handler);
            return Eventually.value(Source.closed("End"));
        };
    }

    public static <A> Source<A> tryCallback(TryEffect1<Effect1<Try0<A,Exception>>,Exception> callbackReceiver) {
        return (closer, handler) -> {
            try {
                callbackReceiver.f(callable -> {
                    try {
                        A a = callable.f();
                        handler.handle(a).execute().await(Duration.ofSeconds(10));
                    } catch (Exception e) {
                       throw new RuntimeException("Exception while producing value",e);
                    }
                });
            } catch (Exception e) {
                return Eventually.fail(e);
            }
            return Eventually.value(Source.closed("End"));
        };
    }

    public static <A> Task<Option<A>> last(Source<A> source){
        AtomicReference<A> aAtomicReference =
          new AtomicReference<>();

        return
          source
            .apply(a-> Task.runnableTask(()->aAtomicReference.set(a)))
            .toTask()
            .andThen(Task.call(()->Option.fromNull(aAtomicReference.get())));

    }

}
