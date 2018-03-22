package org.kantega.niagara.op;

import fj.P;
import fj.P2;
import org.kantega.niagara.Plan;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.RepeatEndBlock;

public class RepeatOp<A> implements Op<A, A> {

    final Plan<A> repeat;

    public RepeatOp(Plan<A> repeat) {
        this.repeat = repeat;
    }

    @Override
    public <C> Op<A, C> fuse(Op<A, C> other) {
        return new ComposedOp<>(this, other);
    }

    @Override
    public P2<Scope, Block<A>> build(Scope scope, Block<A> block) {
        return P.p(scope.child(), new RepeatEndBlock<>(repeat, block));
    }
}
