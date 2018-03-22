package org.kantega.niagara.op;

import fj.P;
import fj.P2;
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
    public P2<Scope, Block<A>> build(Scope scope, Block<B> block) {
        return P.p(scope,new EvalBlock<>(function,block));
    }
}
