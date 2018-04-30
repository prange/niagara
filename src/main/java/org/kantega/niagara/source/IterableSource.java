package org.kantega.niagara.source;

import org.kantega.niagara.Emitter;
import org.kantega.niagara.Source;
import org.kantega.niagara.sink.Sink;

import java.util.Iterator;

public class IterableSource<O> implements Source<O> {
    final Iterable<O> iterable;

    public IterableSource(Iterable<O> iterable) {
        this.iterable = iterable;
    }

    @Override
    public Emitter build(Sink<O> sink) {
        Iterator<O> i = iterable.iterator();
        return () -> {
            if (i.hasNext())
                sink.consumer.accept(i.next());
            else
                sink.done.done(Source.nil());

            return true;
        };
    }

    @Override
    public String toString() {
        return "IterableSource{" +
          iterable +
          '}';
    }
}
