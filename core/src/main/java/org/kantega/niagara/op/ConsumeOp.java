package org.kantega.niagara.op;

import org.kantega.niagara.sink.ConsumerConsumer;
import org.kantega.niagara.state.Scope;

import java.util.function.Consumer;

public class ConsumeOp<A> implements KeepTypeOp<A> {

    final Consumer<A> consumer;

    public ConsumeOp(Consumer<A> consumer) {
        this.consumer = consumer;
    }


    @Override
    public Scope<A> build(Scope<A> input) {
        return Scope.scope(new ConsumerConsumer<>(consumer, input.consumer), input.done.comap(this));

    }
}
