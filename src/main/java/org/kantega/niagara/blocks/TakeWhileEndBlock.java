package org.kantega.niagara.blocks;

import org.kantega.niagara.op.Scope;

import java.util.function.Predicate;

public class TakeWhileEndBlock<A> implements Block<A> {

    final Scope scope;
    final Predicate<A> pred;
    final Block<A> inner;

    public TakeWhileEndBlock(Scope scope, Predicate<A> pred, Block<A> inner) {
        this.scope = scope;
        this.pred = pred;
        this.inner = inner;
    }

    @Override
    public void run(A a) {
        if (pred.test(a))
            inner.run(a);
        else
            scope.halt();

    }
}
