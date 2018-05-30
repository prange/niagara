package org.kantega.niagara.op;

import org.kantega.niagara.sink.ConsumerConsumer;
import org.kantega.niagara.state.Scope;

import java.util.Queue;

public class OfferQueueDroppingOp<A> implements KeepTypeOp<A> {

    final Queue<A> queue;

    public OfferQueueDroppingOp(Queue<A> queue) {
        this.queue = queue;
    }


    @Override
    public Scope<A> build(Scope<A> input) {
        return Scope.scope(new ConsumerConsumer<>(queue::offer, input.consumer), input.done.comap(this));
    }
}
