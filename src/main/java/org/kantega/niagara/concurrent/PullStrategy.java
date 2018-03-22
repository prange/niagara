package org.kantega.niagara.concurrent;

import org.kantega.niagara.op.ScopeFlag;
import org.kantega.niagara.exchange.BackoffPull;
import org.kantega.niagara.exchange.RepeatPull;

import java.util.function.Supplier;

public interface PullStrategy<A> {


    A pull(ScopeFlag abort, Supplier<A> supplier);

    static <A> PullStrategy<A> repeat(WaitStrategy waitStrategy) {
        return new RepeatPull<>(waitStrategy);
    }

    static <A> PullStrategy<A> backoff(long maxSpins, long maxYields, long minParkPeriodNs, long maxParkPeriodNs) {
        return new BackoffPull<>(maxSpins, maxYields, maxParkPeriodNs, maxParkPeriodNs);
    }

}
