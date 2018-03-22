package org.kantega.niagara.blocks;

import fj.Unit;
import org.kantega.niagara.op.ScopeFlag;

import java.util.Iterator;

public class IterableBlock<A> implements Block<Unit> {

    final Iterable<A> source;
    final Block<A> inner;
    final ScopeFlag stopSignal;

    public IterableBlock(ScopeFlag signal, Iterable<A> source, Block<A> inner) {
        this.source = source;
        this.inner = inner;
        this.stopSignal = signal;
    }

    @Override
    public void run(Unit input) {
        Iterator<A> i = source.iterator();
        while (stopSignal.keepRunning() && i.hasNext())
            inner.run(i.next());

    }
}
