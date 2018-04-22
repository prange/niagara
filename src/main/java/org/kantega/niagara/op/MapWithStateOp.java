package org.kantega.niagara.op;

import org.kantega.niagara.Source;
import org.kantega.niagara.sink.MapWithStateSink;

import java.util.function.BiFunction;

public class MapWithStateOp<S,A> implements StageOp<A,S> {

    final S initState;
    final BiFunction<S, A, S> mapFunction;

    public MapWithStateOp(S initState, BiFunction<S, A, S> mapFunction) {
        this.initState = initState;
        this.mapFunction = mapFunction;
    }


    @Override
    public Source<S> apply0(Source<A> input) {
        return (sink,done)->input.build(new MapWithStateSink<>(initState, mapFunction,sink),done.comap(this));
    }
}
