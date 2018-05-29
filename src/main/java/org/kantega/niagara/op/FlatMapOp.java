package org.kantega.niagara.op;

import org.kantega.niagara.sink.FlatMapConsumer;
import org.kantega.niagara.state.Scope;

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
    public Scope<A> build(Scope<B> input) {
        return Scope.scope(
          new FlatMapConsumer<>(function, input.consumer),
          input.done.comap(this));
    }


}
