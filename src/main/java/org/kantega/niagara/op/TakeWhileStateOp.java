package org.kantega.niagara.op;

import org.kantega.niagara.Source;
import org.kantega.niagara.sink.TakeWhileStateSink;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public class TakeWhileStateOp<S, A> implements KeepTypeOp<A> {

    final S state;
    final BiFunction<S, A, S> stateUpdate;
    final Predicate<S> checkState;

    public TakeWhileStateOp(S state, BiFunction<S, A, S> stateUpdate, Predicate<S> checkState) {
        this.state = state;
        this.stateUpdate = stateUpdate;
        this.checkState = checkState;
    }

    @Override
    public Source<A> apply0(Source<A> input) {
        return (sink, done) -> input.build(new TakeWhileStateSink<>(state, stateUpdate, checkState, sink, done), done.comap(this));
    }
}
