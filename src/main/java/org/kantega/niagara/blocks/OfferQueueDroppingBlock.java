package org.kantega.niagara.blocks;

import java.util.Queue;

public class OfferQueueDroppingBlock<A> implements Block<A> {

    final Queue<A> queue;
    final Block<A> inner;

    public OfferQueueDroppingBlock(Queue<A> queue, Block<A> inner) {
        this.queue = queue;
        this.inner = inner;
    }

    @Override
    public void run(A input) {
        queue.offer(input);
        inner.run(input);
    }
}
