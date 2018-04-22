package org.kantega.niagara.sink;

import java.util.function.Consumer;
import java.util.function.Function;

public class FlatMapSink<A,B> implements Sink<A> {

    final Function<A,Iterable<B>> function;
    final Consumer<B> next;

    public FlatMapSink(Function<A, Iterable<B>> function, Consumer<B> next) {
        this.function = function;
        this.next = next;
    }

    @Override
    public void accept(A a) {
        function.apply(a).forEach(next);
    }
}
