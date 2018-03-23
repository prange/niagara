package org.kantega.niagara.blocks;

import org.kantega.niagara.op.ScopeFlag;

public class TakeBlock<A> implements Block<A> {

    final ScopeFlag flag;
    final long max;
    final Block<A> next;
    volatile long counter;

    public TakeBlock(ScopeFlag flag, long max, Block<A> next) {
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
