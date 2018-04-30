package org.kantega.niagara.op;

import org.kantega.niagara.sink.Sink;
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
    public Sink<A> build(Sink<A> input) {
        return Sink.sink(new TakeWhileStateSink<>(state, stateUpdate, checkState, input.consumer, input.done), input.done.comap(this));
    }
}
