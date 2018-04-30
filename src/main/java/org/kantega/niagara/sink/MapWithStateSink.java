package org.kantega.niagara.sink;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public class MapWithStateSink<S,A> implements Consumer<A> {
    volatile S state;
    final BiFunction<S, A, S> zipFunction;
    final Consumer<S> next;

    public MapWithStateSink(S initState, BiFunction<S, A, S> zipFunction, Consumer<S> next) {
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
