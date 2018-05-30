package org.kantega.niagara.sink;

import java.util.function.Consumer;
import java.util.function.Function;

public class MapConsumer<A,B> implements Consumer<A> {

    final Function<A,B> function;
    final Consumer<B> next;

    public MapConsumer(Function<A, B> function, Consumer<B> next) {
        this.function = function;
        this.next = next;
    }


    @Override
    public void accept(A a) {
        next.accept(function.apply(a));
    }
}
