package org.kantega.niagara.op;

import fj.function.TryEffect1;
import org.kantega.niagara.blocks.Block;

public class TryConsumeOp<A> implements Op<A,A> {

    final TryEffect1<A,Exception> consumer;

    public TryConsumeOp(TryEffect1<A, Exception> consumer) {
        this.consumer = consumer;
    }

    @Override
    public Block<A> build(Scope scope, Block<A> block) {
        return null;
    }
}
