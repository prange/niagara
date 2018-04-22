package org.kantega.niagara.sink;

import java.util.function.Function;

public class MapSink<A,B> implements Sink<A> {

    final Function<A,B> function;
    final Sink<B> next;

    public MapSink(Function<A, B> function, Sink<B> next) {
        this.function = function;
        this.next = next;
    }


    @Override
    public void accept(A a) {
        next.accept(function.apply(a));
    }
}
