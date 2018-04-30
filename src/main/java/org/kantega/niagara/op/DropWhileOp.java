package org.kantega.niagara.op;

import org.kantega.niagara.sink.DropWhileConsumer;
import org.kantega.niagara.sink.Sink;

import java.util.function.Predicate;

public class DropWhileOp<A> implements KeepTypeOp<A> {

    final Predicate<A> pred;

    public DropWhileOp(Predicate<A> pred) {
        this.pred = pred;
    }

    @Override
    public Sink<A> build(Sink<A> input) {
        return Sink.sink(new DropWhileConsumer<>(pred, input.consumer, input.done), input.done.comap(this));
    }


}
