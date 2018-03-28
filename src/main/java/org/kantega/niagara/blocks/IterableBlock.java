package org.kantega.niagara.blocks;

import fj.Unit;
import org.kantega.niagara.op.Scope;

import java.util.Iterator;

public class IterableBlock<A> implements Block<Unit> {

    final Iterable<A> source;
    final Block<A> inner;
    final Scope scope;

    public IterableBlock(Scope scope, Iterable<A> source, Block<A> inner) {
        this.source = source;
        this.inner = inner;
        this.scope = scope;
    }

    @Override
    public void run(Unit input) {
        Iterator<A> i = source.iterator();
        while (scope.keepRunning() && i.hasNext())
            inner.run(i.next());

    }
}
