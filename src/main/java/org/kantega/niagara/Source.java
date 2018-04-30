package org.kantega.niagara;

import org.kantega.niagara.sink.Sink;
import org.kantega.niagara.source.IterableSource;
import org.kantega.niagara.source.NilSource;
import org.kantega.niagara.source.QueueSource;
import org.kantega.niagara.source.SingleValueSource;

import java.util.Arrays;
import java.util.Queue;

public interface Source<O> {

    Emitter build(Sink<O> sink);

    static <O> Source<O> single(O value) {
        return new SingleValueSource<>(value);
    }

    static <O> Source<O> emit(O... values) {
        return values.length == 0 ? nil() : iterable(Arrays.asList(values));
    }

    static <O> Source<O> iterable(Iterable<O> iterable) {
        return new IterableSource<>(iterable);
    }

    static <O> Source<O> nil() {
        return new NilSource<>();
    }

    static <O> Source<O> queue(Queue<O> q) {
        return new QueueSource<>(q);
    }

    default boolean isNil() {
        return this instanceof NilSource;
    }


}
