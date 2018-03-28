package org.kantega.niagara.op;

import org.kantega.niagara.Eval;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.EvalBlock;

import java.util.function.Function;

public class EvalOp<A,B> implements Op<A,B> {

    final Function<A,Eval<B>> function;

    public EvalOp(Function<A, Eval<B>> function) {
        this.function = function;
    }

    @Override
    public Block<A> build(Scope scope, Block<B> block) {
        return new EvalBlock<>(function,block);
    }
}
