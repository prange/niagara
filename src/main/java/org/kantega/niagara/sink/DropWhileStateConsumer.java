package org.kantega.niagara.sink;

import org.kantega.niagara.Source;
import org.kantega.niagara.source.Done;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DropWhileStateConsumer<S, A> implements Consumer<A> {
    S state;
    final BiFunction<S, A, S> stateUpdate;
    final Predicate<S> checkState;
    final Consumer<A> next;
    final Done<A> done;
    Consumer<A> use;

    public DropWhileStateConsumer(S state, BiFunction<S, A, S> stateUpdate, Predicate<S> checkState, Consumer<A> next, Done<A> done) {
        this.state = state;
        this.stateUpdate = stateUpdate;
        this.checkState = checkState;
        this.next = next;
        this.done = done;
        this.use = this::acceptTesting;
    }

    @Override
    public void accept(A a) {
        use.accept(a);
    }

    public void acceptTesting(A a) {
        state = stateUpdate.apply(state, a);
        if (!checkState.test(state)) {
            next.accept(a);
            use = next;
        }
    }
}
