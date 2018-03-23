package org.kantega.niagara.blocks;

import fj.Unit;
import org.kantega.niagara.thread.WaitStrategy;
import org.kantega.niagara.op.ScopeFlag;

import java.util.Queue;

public class PullRepeatBlock<A> implements Block<Unit> {

    final ScopeFlag flag;
    final Queue<A> supplier;
    final WaitStrategy waitStrategy;
    final WaitStrategy idleStrategy;
    final Block<A> inner;

    public PullRepeatBlock(ScopeFlag flag, Queue<A> supplier, WaitStrategy waitStrategy, WaitStrategy idleStrategy, Block<A> inner) {
        this.flag = flag;
        this.supplier = supplier;
        this.waitStrategy = waitStrategy;
        this.idleStrategy = idleStrategy;
        this.inner = inner;
    }

    @Override
    public void run(Unit input) {
        while (flag.keepRunning()) {
            A value;
            while ((value = supplier.poll()) == null && flag.keepRunning()) {
                waitStrategy.idle();
            }
            inner.run(value);
            waitStrategy.reset();
            idleStrategy.idle();
        }

    }
}
