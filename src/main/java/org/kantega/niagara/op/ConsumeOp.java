package org.kantega.niagara.op;

import org.kantega.niagara.sink.ConsumerConsumer;
import org.kantega.niagara.sink.Sink;

import java.util.function.Consumer;

public class ConsumeOp<A> implements KeepTypeOp<A> {

    final Consumer<A> consumer;

    public ConsumeOp(Consumer<A> consumer) {
        this.consumer = consumer;
    }


    @Override
    public Sink<A> build(Sink<A> input) {
        return Sink.sink(new ConsumerConsumer<>(consumer, input.consumer), input.done.comap(this));

    }
}
