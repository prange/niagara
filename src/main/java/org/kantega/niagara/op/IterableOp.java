package org.kantega.niagara.op;

import fj.Unit;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.IterableBlock;

public class IterableOp<A> implements Op<Unit, A> {

    final Iterable<A> source;

    public IterableOp(Iterable<A> source) {
        this.source = source;
    }

    @Override
    public Block<Unit> build(Scope scope, Block<A> block) {
        return new IterableBlock<>(scope, source, block);
    }
}
