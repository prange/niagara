package org.kantega.niagara.op;

import org.kantega.niagara.sink.ConsumerConsumer;
import org.kantega.niagara.sink.Sink;

import java.util.Queue;

public class OfferQueueDroppingOp<A> implements KeepTypeOp<A> {

    final Queue<A> queue;

    public OfferQueueDroppingOp(Queue<A> queue) {
        this.queue = queue;
    }


    @Override
    public Sink<A> build(Sink<A> input) {
        return Sink.sink(new ConsumerConsumer<>(queue::offer, input.consumer), input.done.comap(this));
    }
}
