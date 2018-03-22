package org.kantega.niagara;

import org.kantega.niagara.op.ScopeFlag;
import org.kantega.niagara.concurrent.OfferStategy;

import java.util.Queue;
import java.util.function.Consumer;

public interface Offer<A> {

    Eval<A> offer(A value);


    static <A> Offer<A> offer(Queue<A> queue, OfferStategy<A> strategy, ScopeFlag signal) {
        return value -> Eval.call(() -> {
            strategy.offer(signal, value, a -> queue.offer(value));
            return value;
        });
    }

    static <A> Offer<A> consume(Consumer<A> c) {
        return value -> Eval.call(() -> {
            c.accept(value);
            return value;
        });
    }

}
