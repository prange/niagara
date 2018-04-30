package org.kantega.niagara.sink;

import org.kantega.niagara.source.Done;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class DropWhileConsumer<O> implements Consumer<O> {

    final Predicate<O> predicate;
    final Consumer<O> next;
    private Consumer<O> use;
    final Done<O> done;

    public DropWhileConsumer(Predicate<O> predicate, Consumer<O> next, Done<O> done) {
        this.predicate = predicate;
        this.next = next;
        this.done = done;
        this.use = o -> {
            if (!predicate.test(o))
                use = next;
        };
    }

    @Override
    public void accept(O o) {
        use.accept(o);
    }
}
