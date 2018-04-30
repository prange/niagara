package org.kantega.niagara.op;

import org.kantega.niagara.sink.MapConsumer;
import org.kantega.niagara.sink.Sink;

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
    public Sink<A> build(Sink<B> input) {
        return Sink.sink(
          new MapConsumer<>(function, input.consumer),
          input.done.comap(this));
    }

    @Override
    public String toString() {
        return "MapOp{}";
    }
}
