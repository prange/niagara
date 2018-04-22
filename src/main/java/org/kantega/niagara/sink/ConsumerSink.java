package org.kantega.niagara.sink;

import java.util.function.Consumer;

public class ConsumerSink<O> implements Sink<O> {

    final Consumer<O> consumer;
    final Sink<O> next;

    public ConsumerSink(Consumer<O> consumer, Sink<O> next) {
        this.consumer = consumer;
        this.next = next;
    }

    @Override
    public void accept(O o) {
        consumer.accept(o);
        next.accept(o);
    }
}
