package org.kantega.niagara.concurrent;

import org.kantega.niagara.op.ScopeFlag;

import java.util.function.Function;

public interface OfferStategy<A> {

    void offer(ScopeFlag signal, A value, Function<A, Boolean> sink);

}
