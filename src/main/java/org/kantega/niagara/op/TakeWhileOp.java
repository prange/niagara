package org.kantega.niagara.op;

import fj.P;
import fj.P2;
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
        return new ComposedOp<>(this, other);
    }

    @Override
    public P2<Scope, Block<A>> build(Scope scope, Block<A> block) {
        Scope child = scope.child();
        return P.p(child, new TakeWhileEndBlock<>(child.getFlag(), pred, block));
    }
}
