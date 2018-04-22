package org.kantega.niagara.op;

import org.kantega.niagara.Source;
import org.kantega.niagara.sink.MapSink;

import java.util.function.Function;

public class MapOp<A, B> implements StageOp<A, B> {

    final Function<A, B> function;

    public MapOp(Function<A, B> function) {
        this.function = function;
    }


    @Override
    public <C> StageOp<A, C> fuse(StageOp<B, C> other) {
        if (other instanceof MapOp)
            return new MapOp<>(function.andThen(((MapOp<B, C>) other).function));
        if (other instanceof FlatMapOp)
            return new FlatMapOp<>(function.andThen(((FlatMapOp<B, C>) other).function));
        return StageOp.super.fuse(other);
    }

    @Override
    public Source<B> apply0(Source<A> input) {
        return (emit, done) -> input.build(
          new MapSink<>(function, emit),
          done.comap(this));
    }


}
