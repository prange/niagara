package org.kantega.niagara.op;

import org.kantega.niagara.Plan;
import org.kantega.niagara.blocks.BindBlock;
import org.kantega.niagara.blocks.Block;

import java.util.function.Function;

public class BindOp<A, B> implements StageOp<A, B> {


    final Function<A, Plan<B>> function;

    public BindOp(Function<A, Plan<B>> function) {
        this.function = function;
    }

    @Override
    public Block<A> build(Scope scope, Block<B> block) {
        return new BindBlock<>(scope, function, block);
    }
}
