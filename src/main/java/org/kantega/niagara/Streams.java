package org.kantega.niagara;

import fj.data.List;
import fj.function.Effect1;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.SynchronousQueue;

public class Streams {

    public static <A> Stream<A> wrap(Eventually<Stream.Next<A>> awaiting) {
        return (stop) -> awaiting;
    }

    public static <A> Stream<A> empty() {
        return (stop) -> Eventually.value(Stream.end());
    }

    public static <A> Stream<A> value(A a) {
        return (stop) -> Eventually.value(Stream.cont(a, () -> empty()));
    }

    public static <A> Stream<A> values(A a, A... as) {
        return (stop) ->
          Eventually.value(Stream.cont(a, () -> values(List.arrayList(as))));

    }

    public static <A> Stream<A> values(List<A> values) {
        return
          values.isEmpty() ?
            empty() :
            (stop) -> Eventually.value(Stream.cont(values.head(), () -> values(values.tail())));
    }

    public static <A> Stream<A> callBack(Effect1<Effect1<A>> callback) {
        return stop -> {
            CallerBacker<A> cb = new CallerBacker<>(stop);

            callback.f(cb::handle);
            cb.exchanger.offer(Stream.end("Stopped"));

            return cb.next();
        };
    }

    private static class CallerBacker<A> {
        final SynchronousQueue<Stream.Next<A>> exchanger = new SynchronousQueue<>();

        final CompletionStage<Stream.Stop> stopCompletionStage;

        private CallerBacker(CompletionStage<Stream.Stop> stopCompletionStage) {
            this.stopCompletionStage = stopCompletionStage;
        }

        void handle(A a) {
            exchanger.offer(Stream.cont(a, () -> wrap(next())));
        }

        Eventually<Stream.Next<A>> next() {
            if (stopCompletionStage.toCompletableFuture().isDone()) {
                return Eventually.value(Stream.end("Stopped"));
            } else {
                try {
                    return Eventually.value(exchanger.take());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
