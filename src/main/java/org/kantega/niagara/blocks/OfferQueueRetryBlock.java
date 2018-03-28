package org.kantega.niagara.blocks;

import org.kantega.niagara.op.Scope;
import org.kantega.niagara.thread.WaitStrategy;

import java.util.Queue;

public class OfferQueueRetryBlock<A> implements Block<A> {

    final Queue<A> queue;
    final WaitStrategy strategy;
    final Scope scope;
    final Block<A> inner;

    public OfferQueueRetryBlock(Queue<A> queue, WaitStrategy strategy, Scope scope, Block<A> inner) {
        this.queue = queue;
        this.strategy = strategy;
        this.scope = scope;
        this.inner = inner;
    }


    @Override
    public void run(A input) {
        while (scope.keepRunning() && queue.offer(input))
            strategy.idle();

        strategy.reset();
        inner.run(input);
    }
}
