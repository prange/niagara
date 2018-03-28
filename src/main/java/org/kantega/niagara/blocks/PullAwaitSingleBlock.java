package org.kantega.niagara.blocks;

import fj.Unit;
import org.kantega.niagara.op.Scope;
import org.kantega.niagara.thread.WaitStrategy;

import java.util.Queue;

public class PullAwaitSingleBlock<A> implements Block<Unit> {

    final Queue<A> queue;
    final WaitStrategy waitStrategy;
    final Scope scope;
    final Block<A> next;

    public PullAwaitSingleBlock(Queue<A> queue, WaitStrategy waitStrategy, Scope scope, Block<A> next) {
        this.queue = queue;
        this.waitStrategy = waitStrategy;
        this.scope = scope;
        this.next = next;
    }

    @Override
    public void run(Unit input) {
        A value;
        while ((value = queue.poll()) == null && scope.keepRunning()) {
            waitStrategy.idle();
        }
        waitStrategy.reset();
        next.run(value);
    }
}
