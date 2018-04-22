package org.kantega.niagara.op;

public class RepeatOp<A> implements Ops<A> {

    final Ops<A> repeated;

    public RepeatOp(Ops<A> repeated) {
        this.repeated = repeated;
    }


    @Override
    public <O2> Ops<O2> append(StageOp<A, O2> next) {
        return new Ops.OpChain<>(this, next);
    }
}
