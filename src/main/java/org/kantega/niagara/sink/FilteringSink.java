package org.kantega.niagara.sink;

import java.util.function.Predicate;

public class FilteringSink<A> implements Sink<A> {

    final Predicate<A> predicate;
    final Sink<A> next;

    public FilteringSink(Predicate<A> predicate, Sink<A> next) {
        this.predicate = predicate;
        this.next = next;
    }

    @Override
    public void accept(A a) {
        if (predicate.test(a))
            next.accept(a);
    }
}
