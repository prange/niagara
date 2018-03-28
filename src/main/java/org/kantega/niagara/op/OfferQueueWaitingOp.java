package org.kantega.niagara.op;

import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.OfferQueueRetryBlock;
import org.kantega.niagara.thread.WaitStrategy;

import java.util.Queue;

public class OfferQueueWaitingOp<A> implements Op<A, A> {

    final Queue<A> queue;
    final WaitStrategy waitStrategy;

    public OfferQueueWaitingOp(Queue<A> queue, WaitStrategy waitStrategy) {
        this.queue = queue;
        this.waitStrategy = waitStrategy;
    }

    @Override
    public Block<A> build(Scope scope, Block<A> block) {
        return new OfferQueueRetryBlock<>(queue, waitStrategy, scope, block);
    }
}