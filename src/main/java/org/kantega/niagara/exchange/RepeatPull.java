package org.kantega.niagara.exchange;

import org.kantega.niagara.op.ScopeFlag;
import org.kantega.niagara.concurrent.PullStrategy;
import org.kantega.niagara.concurrent.WaitStrategy;

import java.util.function.Supplier;

public class RepeatPull<A> implements PullStrategy<A> {

    final WaitStrategy waitStrategy;

    public RepeatPull(WaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
    }

    @Override
    public A pull(ScopeFlag s, Supplier<A> supplier) {
        A value;
        while ((value = supplier.get()) == null && s.keepRunning()) {
            waitStrategy.idle();
        }
        return value;
    }
}
