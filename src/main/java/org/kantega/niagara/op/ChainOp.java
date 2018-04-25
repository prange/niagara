package org.kantega.niagara.op;

import org.kantega.niagara.Source;

public class ChainOp<A, B, C> implements StageOp<A, C> {

    final StageOp<A, B> op1;
    final StageOp<B, C> op2;

    public ChainOp(StageOp<A, B> op1, StageOp<B, C> op2) {
        this.op1 = op1;
        this.op2 = op2;
    }

    @Override
    public <D> StageOp<A, D> fuse(StageOp<C, D> other) {
        return new ChainOp<>(op1, op2.fuse(other));
    }

    @Override
    public Source<C> apply0(Source<A> input) {
        return op2.apply(op1.apply(input));
    }


}
