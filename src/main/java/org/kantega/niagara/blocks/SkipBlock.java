package org.kantega.niagara.blocks;

public class SkipBlock<A> implements Block<A> {

    volatile long counter = 0;
    final long skip;
    final Block<A> next;

    public SkipBlock(long skip, Block<A> next) {
        this.skip = skip;
        this.next = next;
    }

    @Override
    public void run(A input) {
        if (counter > skip)
            next.run(input);
        counter += 1;
    }
}
