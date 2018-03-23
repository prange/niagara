package org.kantega.niagara.op;

import fj.P;
import fj.P2;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.TakeBlock;

public class TakeOp<A> implements Op<A, A> {

    final long count;

    public TakeOp(long count) {
        this.count = count;
    }

    @Override
    public P2<Scope, Block<A>> build(Scope scope, Block<A> block) {
        return P.p(scope, new TakeBlock<>(scope.getFlag(), count, block));
    }
}
