package org.kantega.niagara.source;

import org.kantega.niagara.Source;
import org.kantega.niagara.Emitter;
import org.kantega.niagara.sink.Sink;

import java.util.Iterator;

public class IterableSource<O> implements Source<O> {
    final Iterable<O> iterable;

    public IterableSource(Iterable<O> iterable) {
        this.iterable = iterable;
    }

    @Override
    public Emitter build(Sink<O> emit, Done<O> done) {
        Iterator<O> i = iterable.iterator();
        return () -> {
            if (i.hasNext())
                emit.accept(i.next());
            else
                done.done(Source.nil());

            return true;
        };
    }
}
