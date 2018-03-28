package org.kantega.niagara.op;

import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.FlatMapBlock;

import java.util.function.Function;

public class FlatMapOp<A, B> implements Op<A, B> {

    final Function<A, Iterable<B>> f;

    public FlatMapOp(Function<A, Iterable<B>> f) {
        this.f = f;
    }

    @Override
    public <C> Op<A, C> fuse(Op<B, C> other) {
        return new AndThenOp<>(this, other);
    }

    @Override
    public Block<A> build(Scope scope, Block<B> block) {
        return new FlatMapBlock<>(scope, f, block);
    }
}
