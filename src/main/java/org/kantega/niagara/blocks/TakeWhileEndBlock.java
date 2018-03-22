package org.kantega.niagara.blocks;

import org.kantega.niagara.op.ScopeFlag;

import java.util.function.Predicate;

public class TakeWhileEndBlock<A> implements Block<A> {

    final ScopeFlag stopCommand;
    final Predicate<A> pred;
    final Block<A> inner;

    public TakeWhileEndBlock(ScopeFlag stopCommand, Predicate<A> pred, Block<A> inner) {
        this.stopCommand = stopCommand;
        this.pred = pred;
        this.inner = inner;
    }

    @Override
    public void run( A a) {
        if (pred.test(a))
            stopCommand.halt();
        else
            inner.run(a);
    }
}
