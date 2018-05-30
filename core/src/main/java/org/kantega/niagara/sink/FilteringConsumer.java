package org.kantega.niagara.sink;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class FilteringConsumer<A> implements Consumer<A> {

    final Predicate<A> predicate;
    final Consumer<A> next;

    public FilteringConsumer(Predicate<A> predicate, Consumer<A> next) {
        this.predicate = predicate;
        this.next = next;
    }

    @Override
    public void accept(A a) {
        if (predicate.test(a))
            next.accept(a);
    }
}
