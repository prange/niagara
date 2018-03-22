package org.kantega.niagara.blocks;

import org.kantega.niagara.Plan;

public class RepeatEndBlock<A> implements Block<A> {

    final Plan<A> repeat;
    final Block<A> nested;

    public RepeatEndBlock(Plan<A> repeat, Block<A> nested) {
        this.repeat = repeat;
        this.nested = nested;
    }

    @Override
    public void run(A input) {
        nested.run(input);

    }
}
