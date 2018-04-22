package org.kantega.niagara.sink;

import org.kantega.niagara.Source;
import org.kantega.niagara.source.Done;

import java.util.function.Predicate;

public class DropWhileSink<O> implements Sink<O> {

    final Predicate<O> predicate;
    final Source<O> next;
    final Done<O> done;

    public DropWhileSink(Predicate<O> predicate, Source<O> next, Done<O> done) {
        this.predicate = predicate;
        this.next = next;
        this.done = done;
    }

    @Override
    public void accept(O o) {
        if (!predicate.test(o))
            done.done(next);
    }
}
