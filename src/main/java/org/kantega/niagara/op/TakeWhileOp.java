package org.kantega.niagara.op;

import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.TakeWhileEndBlock;

import java.util.function.Predicate;

public class TakeWhileOp<A> implements Op<A, A> {

    final Predicate<A> pred;

    public TakeWhileOp(Predicate<A> pred) {
        this.pred = pred;
    }

    @Override
    public <C> Op<A, C> fuse(Op<A, C> other) {
        return new AndThenOp<>(this, other);
    }

    @Override
    public  Block<A> build(Scope scope, Block<A> block) {
        return new TakeWhileEndBlock<>(scope, pred, block);
    }
}
