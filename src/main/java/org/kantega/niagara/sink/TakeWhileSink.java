package org.kantega.niagara.sink;

import org.kantega.niagara.Source;
import org.kantega.niagara.sink.Sink;
import org.kantega.niagara.source.Done;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class TakeWhileSink<O> implements Sink<O> {

    final Predicate<O> predicate;
    final Consumer<O> next;
    final Done<O> done;

    public TakeWhileSink(Predicate<O> predicate, Consumer<O> next, Done<O> done) {
        this.predicate = predicate;
        this.next = next;
        this.done = done;
    }


    @Override
    public void accept(O o) {
        if (predicate.test(o))
            next.accept(o);
        else
            done.done(Source.nil());
    }
}
