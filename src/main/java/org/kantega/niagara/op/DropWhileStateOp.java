package org.kantega.niagara.op;

import org.kantega.niagara.sink.DropWhileStateConsumer;
import org.kantega.niagara.state.Scope;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public class DropWhileStateOp<S,A> implements KeepTypeOp<A> {

    final S initState;
    final BiFunction<S,A,S> updateState;
    final Predicate<S> checkState;

    public DropWhileStateOp(S initState, BiFunction<S, A, S> updateState, Predicate<S> checkState) {
        this.initState = initState;
        this.updateState = updateState;
        this.checkState = checkState;
    }

    @Override
    public Scope<A> build(Scope<A> input) {
        return
          Scope.scope(new DropWhileStateConsumer<>(initState,updateState,checkState,input.consumer,input.done),input.done.comap(this));
    }
}
