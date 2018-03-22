package org.kantega.niagara.op;

import fj.P;
import fj.P2;
import org.kantega.niagara.Plan;
import org.kantega.niagara.blocks.BindBlock;
import org.kantega.niagara.blocks.Block;

import java.util.function.Function;

public class BindOp<A, B> implements Op<A, B> {


    final Function<A, Plan<B>> function;

    public BindOp(Function<A, Plan<B>> function) {
        this.function = function;
    }

    @Override
    public P2<Scope, Block<A>> build(Scope scope, Block<B> block) {
        return P.p(scope, new BindBlock<>(scope, function, block));
    }
}
