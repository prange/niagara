package org.kantega.niagara.op;

import org.kantega.niagara.Source;
import org.kantega.niagara.sink.ConsumerSink;
import org.kantega.niagara.thread.WaitStrategy;

import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class OfferQueueWaitingOp<A> implements StageOp<A, A> {

    final Queue<A> queue;
    final Supplier<WaitStrategy> waitStrategy;

    public OfferQueueWaitingOp(Queue<A> queue, Supplier<WaitStrategy> waitStrategy) {
        this.queue = queue;
        this.waitStrategy = waitStrategy;
    }


    @Override
    public Source<A> apply0(Source<A> input) {
        Consumer<A> consumer = new Consumer<>() {

            WaitStrategy strategy = waitStrategy.get();

            @Override
            public void accept(A a) {
                while (!queue.offer(a)) {
                    strategy.idle();
                }
                strategy.reset();
            }
        };

        return (emit, done) ->
          input.build(new ConsumerSink<>(consumer, emit), done.comap(this));
    }
}