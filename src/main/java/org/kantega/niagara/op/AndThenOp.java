package org.kantega.niagara.op;

import org.kantega.niagara.blocks.Block;

public class AndThenOp<A, B, C> implements Op<A, C> {

    final Op<A, B> op1;
    final Op<B, C> op2;

    public AndThenOp(Op<A, B> op1, Op<B, C> op2) {
        this.op1 = op1;
        this.op2 = op2;
    }

    @Override
    public <D> Op<A, D> fuse(Op<C, D> other) {
        return new AndThenOp<>(op1, op2.fuse(other));
    }

    @Override
    public Block<A> build(Scope scope, Block<C> block) {
        Block<B> next = op2.build(scope, block);
        return op1.build(scope, next);
    }
}
