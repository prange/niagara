package org.kantega.niagara.exchange;

import org.kantega.niagara.op.ScopeFlag;
import org.kantega.niagara.concurrent.OfferStategy;
import org.kantega.niagara.concurrent.WaitStrategy;

import java.util.function.Function;

public class RepeatOffer<A> implements OfferStategy<A> {


    private final WaitStrategy waitStrategy;

    public RepeatOffer(WaitStrategy strategy) {
        this.waitStrategy = strategy;
    }


    @Override
    public void offer(ScopeFlag signal, A value, Function<A, Boolean> sink) {
        while (!sink.apply(value) && signal.keepRunning()) {
            waitStrategy.idle();
        }
    }
}
