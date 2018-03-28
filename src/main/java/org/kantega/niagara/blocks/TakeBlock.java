package org.kantega.niagara.blocks;

import org.kantega.niagara.op.Scope;

public class TakeBlock<A> implements Block<A> {

    final Scope flag;
    final long max;
    final Block<A> next;
    volatile long counter;

    public TakeBlock(Scope flag, long max, Block<A> next) {
        this.flag = flag;
        this.max = max;
        this.next = next;
    }

    @Override
    public void run(A input) {
        if (counter < max) {
            next.run(input);
        } else {
            flag.halt();
        }
        counter += 1;
    }

}
