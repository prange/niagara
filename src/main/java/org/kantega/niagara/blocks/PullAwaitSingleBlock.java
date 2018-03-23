package org.kantega.niagara.blocks;

import fj.Unit;
import org.kantega.niagara.concurrent.WaitStrategy;
import org.kantega.niagara.op.ScopeFlag;

import java.util.Queue;

public class PullAwaitSingleBlock<A> implements Block<Unit> {

    final Queue<A> queue;
    final WaitStrategy waitStrategy;
    final ScopeFlag flag;
    final Block<A> next;

    public PullAwaitSingleBlock(Queue<A> queue, WaitStrategy waitStrategy, ScopeFlag flag, Block<A> next) {
        this.queue = queue;
        this.waitStrategy = waitStrategy;
        this.flag = flag;
        this.next = next;
    }

    @Override
    public void run(Unit input) {
        A value;
        while ((value = queue.poll()) == null && flag.keepRunning()) {
            waitStrategy.idle();
        }
        waitStrategy.reset();
        next.run(value);
    }
}
