package org.kantega.niagara.sink;

import java.util.function.Consumer;

public class ConsumerConsumer<O> implements Consumer<O>{

    final Consumer<O> consumer;
    final Consumer<O> next;

    public ConsumerConsumer(Consumer<O> consumer, Consumer<O> next) {
        this.consumer = consumer;
        this.next = next;
    }

    @Override
    public void accept(O o) {
        consumer.accept(o);
        next.accept(o);
    }
}
