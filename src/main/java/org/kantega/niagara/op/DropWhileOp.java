package org.kantega.niagara.op;

import org.kantega.niagara.sink.DropWhileConsumer;
import org.kantega.niagara.state.Scope;

import java.util.function.Predicate;

public class DropWhileOp<A> implements KeepTypeOp<A> {

    final Predicate<A> pred;

    public DropWhileOp(Predicate<A> pred) {
        this.pred = pred;
    }

    @Override
    public Scope<A> build(Scope<A> input) {
        return Scope.scope(new DropWhileConsumer<>(pred, input.consumer, input.done), input.done.comap(this));
    }


}
