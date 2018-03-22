package org.kantega.niagara.exchange;

import org.kantega.niagara.op.ScopeFlag;
import org.kantega.niagara.concurrent.OfferStategy;

import java.util.function.Function;

public class DropOffer<A> implements OfferStategy<A> {
    @Override
    public void offer(ScopeFlag signal, A value, Function<A, Boolean> sink) {
        sink.apply(value);
    }
}
