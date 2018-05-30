package org.kantega.niagara.sink;

import java.util.function.Consumer;
import java.util.function.Function;

public class FlatMapConsumer<A,B> implements Consumer<A> {

    final Function<A,Iterable<B>> function;
    final Consumer<B> next;

    public FlatMapConsumer(Function<A, Iterable<B>> function, Consumer<B> next) {
        this.function = function;
        this.next = next;
    }

    @Override
    public void accept(A a) {
        function.apply(a).forEach(next);
    }
}
