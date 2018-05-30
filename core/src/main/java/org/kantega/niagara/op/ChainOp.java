package org.kantega.niagara.op;

import org.kantega.niagara.state.Scope;

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
    public Scope<A> build(Scope<C> input) {
        return op1.build(op2.build(input));
    }

    @Override
    public String toString() {
        return "ChainOp{" +
           op1.getClass().getSimpleName() +
          "," + op2.getClass().getSimpleName() +
          '}';
    }
}
