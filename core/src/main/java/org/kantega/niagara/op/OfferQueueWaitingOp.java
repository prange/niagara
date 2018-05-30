package org.kantega.niagara.op;

import org.kantega.niagara.sink.ConsumerConsumer;
import org.kantega.niagara.state.Scope;
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
    public Scope<A> build(Scope<A> input) {
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

        return
          Scope.scope(new ConsumerConsumer<>(consumer, input.consumer), input.done.comap(this));
    }
}