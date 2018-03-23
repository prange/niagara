package org.kantega.niagara.exchange;

import org.kantega.niagara.Eval;
import org.kantega.niagara.concurrent.WaitStrategy;
import org.kantega.niagara.op.ScopeFlag;

import java.util.Queue;
import java.util.function.Consumer;

public interface Offer<A> {

    boolean offer(A a);


    static <A> Offer<A> offer(Queue<A> queue, WaitStrategy strategy, ScopeFlag signal) {
        return new RepeatOffer<>(queue, strategy, signal);
    }


    static <A> Offer<A> consume(Consumer<A> c) {
        return new ConsumerOffer<>(c);
    }

    class ConsumerOffer<A> implements Offer<A> {

        final Consumer<A> consumer;

        public ConsumerOffer(Consumer<A> consumer) {
            this.consumer = consumer;
        }

        @Override
        public boolean offer(A a) {
            consumer.accept(a);
            return true;
        }
    }

    class RepeatOffer<A> implements Offer<A> {
        final Queue<A> queue;
        final WaitStrategy strategy;
        final ScopeFlag signal;

        public RepeatOffer(Queue<A> queue, WaitStrategy strategy, ScopeFlag signal) {
            this.queue = queue;
            this.strategy = strategy;
            this.signal = signal;
        }

        @Override
        public boolean offer(A a) {
            while (!queue.offer(a) && signal.keepRunning()) {
                strategy.idle();
            }
            return true; //TODO yield false when aborted.
        }
    }
}
