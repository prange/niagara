package org.kantega.niagara.op;

import org.kantega.niagara.sink.MapWithStateSink;
import org.kantega.niagara.state.Scope;

import java.util.function.BiFunction;

public class MapWithStateOp<S,A> implements StageOp<A,S> {

    final S initState;
    final BiFunction<S, A, S> mapFunction;

    public MapWithStateOp(S initState, BiFunction<S, A, S> mapFunction) {
        this.initState = initState;
        this.mapFunction = mapFunction;
    }


    @Override
    public Scope<A> build(Scope<S> input) {
        return Scope.scope(new MapWithStateSink<>(initState, mapFunction,input.consumer),input.done.comap(this));
    }
}
