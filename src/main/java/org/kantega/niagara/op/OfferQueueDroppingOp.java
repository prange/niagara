package org.kantega.niagara.op;

import org.kantega.niagara.Source;
import org.kantega.niagara.sink.ConsumerSink;

import java.util.Queue;

public class OfferQueueDroppingOp<A> implements StageOp<A, A> {

    final Queue<A> queue;

    public OfferQueueDroppingOp(Queue<A> queue) {
        this.queue = queue;
    }


    @Override
    public Source<A> apply0(Source<A> input) {
        return (emit, done) ->
          input.build(new ConsumerSink<>(queue::offer, emit), done.comap(this));
    }
}
