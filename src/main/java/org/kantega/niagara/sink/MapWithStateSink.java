package org.kantega.niagara.sink;

import java.util.function.BiFunction;

public class MapWithStateSink<S,A> implements Sink<A> {
    volatile S state;
    final BiFunction<S, A, S> zipFunction;
    final Sink<S> next;

    public MapWithStateSink(S initState, BiFunction<S, A, S> zipFunction, Sink<S> next) {
        this.zipFunction = zipFunction;
        this.state = initState;
        this.next = next;
    }


    @Override
    public void accept(A input) {
        state = zipFunction.apply(state, input);
        next.accept(state);
    }
}
