package org.kantega.niagara.op;

import org.kantega.niagara.sink.FilteringConsumer;
import org.kantega.niagara.state.Scope;

import java.util.function.Predicate;

public class FilterOp<A> implements KeepTypeOp<A> {

    final Predicate<A> predicate;

    public FilterOp(Predicate<A> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Scope<A> build(Scope<A> input) {
        return Scope.scope(new FilteringConsumer<>(predicate, input.consumer), input.done.comap(this));
    }
}
