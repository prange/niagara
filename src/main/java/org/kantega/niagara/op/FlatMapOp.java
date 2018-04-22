package org.kantega.niagara.op;

import org.kantega.niagara.Source;
import org.kantega.niagara.sink.FlatMapSink;

import java.util.function.Function;

public class FlatMapOp<A, B> implements StageOp<A, B> {

    final Function<A, Iterable<B>> function;

    public FlatMapOp(Function<A, Iterable<B>> function) {
        this.function = function;
    }

    @Override
    public <C> StageOp<A, C> fuse(StageOp<B, C> other) {
        return new ChainOp<>(this, other);
    }

    @Override
    public Source<B> apply0(Source<A> input) {
        return (emit, done) -> input.build(
          new FlatMapSink<>(function, emit),
          done.comap(this));
    }


}
