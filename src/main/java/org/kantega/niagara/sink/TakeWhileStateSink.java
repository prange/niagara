package org.kantega.niagara.sink;

import org.kantega.niagara.Source;
import org.kantega.niagara.source.Done;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public class TakeWhileStateSink<S, A> implements Sink<A> {

     S state;
    final BiFunction<S, A, S> stateUpdate;
    final Predicate<S> checkState;
    final Sink<A> next;
    final Done<A> done;

    public TakeWhileStateSink(S state, BiFunction<S, A, S> stateUpdate, Predicate<S> checkState, Sink<A> next, Done<A> done) {
        this.state = state;
        this.stateUpdate = stateUpdate;
        this.checkState = checkState;
        this.next = next;
        this.done = done;
    }

    @Override
    public void accept(A a) {
        state = stateUpdate.apply(state, a);
        if (checkState.test(state))
            next.accept(a);
        else
            done.done(Source.nil());

    }
}
