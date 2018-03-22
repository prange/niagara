package org.kantega.niagara.op;

import fj.P;
import fj.P2;
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
        return new ComposedOp<>(this, other);
    }

    @Override
    public P2<Scope, Block<A>> build(Scope scope, Block<B> block) {
        return P.p(scope, new FlatMapBlock<>(scope.getFlag(), f, block));
    }
}
