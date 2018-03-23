package org.kantega.niagara.blocks;

import org.kantega.niagara.concurrent.WaitStrategy;
import org.kantega.niagara.op.ScopeFlag;

import java.util.Queue;

public class OfferQueueRetryBlock<A> implements Block<A> {

    final Queue<A> queue;
    final WaitStrategy strategy;
    final ScopeFlag signal;
    final Block<A> inner;

    public OfferQueueRetryBlock(Queue<A> queue, WaitStrategy strategy, ScopeFlag signal, Block<A> inner) {
        this.queue = queue;
        this.strategy = strategy;
        this.signal = signal;
        this.inner = inner;
    }


    @Override
    public void run(A input) {
        while (signal.keepRunning() && queue.offer(input))
            strategy.idle();

        strategy.reset();
        inner.run(input);
    }
}
