package org.kantega.niagara.op;

import fj.P2;
import org.kantega.niagara.blocks.Block;

public class ComposedOp<A, B, C> implements Op<A, C> {

    final Op<A, B> op1;
    final Op<B, C> op2;

    public ComposedOp(Op<A, B> op1, Op<B, C> op2) {
        this.op1 = op1;
        this.op2 = op2;
    }

    @Override
    public <D> Op<A, D> fuse(Op<C, D> other) {
        return new ComposedOp<>(op1, op2.fuse(other));
    }

    @Override
    public P2<Scope, Block<A>> build(Scope scope, Block<C> block) {
        P2<Scope, Block<B>> next = op2.build(scope, block);
        return op1.build(next._1(), next._2());
    }
}
