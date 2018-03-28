package org.kantega.niagara.op;

import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.SkipBlock;

public class SkipOp<A> implements Op<A, A> {

    final long skip;

    public SkipOp(long skip) {
        this.skip = skip;
    }

    @Override
    public Block<A> build(Scope scope, Block<A> block) {
        return new SkipBlock<>(skip, block);
    }
}
