package org.kantega.niagara.sink;

import org.kantega.niagara.source.Done;

import java.util.function.Consumer;

public class Sink<O> {

    public final Consumer<O> consumer;
    public final Done<O> done;

    public Sink(Consumer<O> consumer, Done<O> done) {
        this.consumer = consumer;
        this.done = done;
    }


    public static <A> Sink<A> sink(Consumer<A> consumer, Done<A> done) {
        return new Sink<>(consumer, done);
    }

}
