package org.kantega.niagara.blocks;

import fj.Unit;
import org.kantega.niagara.Plan;
import org.kantega.niagara.op.Scope;

import java.util.function.Function;

public class BindBlock<A, B> implements Block<A> {
    final Scope scope;
    final Function<A, Plan<B>> f;
    final Block<B> inner;

    public BindBlock(Scope scope, Function<A, Plan<B>> f, Block<B> inner) {
        this.scope = scope;
        this.f = f;
        this.inner = inner;
    }

    @Override
    public void run(A input) {
        Block<Unit> bblock = f.apply(input).build(scope, inner);
        bblock.run(Unit.unit());

    }
}
