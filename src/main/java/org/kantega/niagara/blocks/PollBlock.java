package org.kantega.niagara.blocks;

import fj.Unit;

import java.util.Queue;

public class PollBlock<A> implements Block<Unit> {

    final Queue<A> queue;
    final Block<A> next;

    public PollBlock(Queue<A> queue, Block<A> next) {
        this.queue = queue;
        this.next = next;
    }


    @Override
    public void run(Unit input) {
        A value = queue.poll();
        if (value != null)
            next.run(value);
    }
}
