package org.kantega.niagara.op;

import fj.P;
import fj.P2;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.SkipBlock;

public class SkipOp<A> implements Op<A, A> {

    final long skip;

    public SkipOp(long skip) {
        this.skip = skip;
    }

    @Override
    public P2<Scope, Block<A>> build(Scope scope, Block<A> block) {
        return P.p(scope, new SkipBlock<>(skip, block));
    }
}
