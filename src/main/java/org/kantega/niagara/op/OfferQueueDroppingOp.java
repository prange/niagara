package org.kantega.niagara.op;

import fj.P;
import fj.P2;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.OfferQueueDroppingBlock;

import java.util.Queue;

public class OfferQueueDroppingOp<A> implements Op<A, A> {

    final Queue<A> queue;

    public OfferQueueDroppingOp(Queue<A> queue) {
        this.queue = queue;
    }

    @Override
    public P2<Scope, Block<A>> build(Scope scope, Block<A> block) {
        return P.p(scope, new OfferQueueDroppingBlock<>(queue, block));
    }
}
