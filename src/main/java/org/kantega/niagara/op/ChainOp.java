package org.kantega.niagara.op;

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


}
