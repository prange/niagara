package org.kantega.niagara.op;

import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.OfferQueueDroppingBlock;

import java.util.Queue;

public class OfferQueueDroppingOp<A> implements Op<A, A> {

    final Queue<A> queue;

    public OfferQueueDroppingOp(Queue<A> queue) {
        this.queue = queue;
    }

    @Override
    public Block<A> build(Scope scope, Block<A> block) {
        return new OfferQueueDroppingBlock<>(queue, block);
    }
}
