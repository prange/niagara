package org.kantega.niagara.blocks;

import org.kantega.niagara.op.ScopeFlag;

import java.util.Iterator;
import java.util.function.Function;

public class FlatMapBlock<A, B> implements Block<A> {

    final Block<B> inner;
    final Function<A, Iterable<B>> f;
    final ScopeFlag scope;

    public FlatMapBlock(ScopeFlag scope, Function<A, Iterable<B>> f, Block<B> inner) {
        this.f = f;
        this.inner = inner;
        this.scope = scope;
    }

    @Override
    public void run(A input) {
        Iterator<B> iter = f.apply(input).iterator();
        while (scope.keepRunning() && iter.hasNext())
            inner.run(iter.next());
    }
}
