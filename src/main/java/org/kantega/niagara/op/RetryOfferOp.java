package org.kantega.niagara.op;

import fj.P;
import fj.P2;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.OfferQueueRetryBlock;
import org.kantega.niagara.concurrent.WaitStrategy;

import java.util.Queue;

public class RetryOfferOp<A> implements Op<A, A> {

    final Queue<A> queue;
    final WaitStrategy strategy;
    final ScopeFlag signal;

    public RetryOfferOp(Queue<A> queue, WaitStrategy strategy, ScopeFlag signal) {
        this.queue = queue;
        this.strategy = strategy;
        this.signal = signal;
    }


    @Override
    public P2<Scope, Block<A>> build(Scope scope, Block<A> block) {
        return P.p(scope, new OfferQueueRetryBlock<>(queue, strategy, signal, block));
    }
}
