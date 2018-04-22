package org.kantega.niagara.op;

import org.kantega.niagara.Source;
import org.kantega.niagara.sink.DropWhileStateSink;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public class DropWhileStateOp<S,A> implements KeepTypeOp<A> {

    final S initState;
    final BiFunction<S,A,S> updateState;
    final Predicate<S> checkState;

    public DropWhileStateOp(S initState, BiFunction<S, A, S> updateState, Predicate<S> checkState) {
        this.initState = initState;
        this.updateState = updateState;
        this.checkState = checkState;
    }

    @Override
    public Source<A> apply0(Source<A> input) {
        return (sink,done)->input.build(new DropWhileStateSink<>(initState,updateState,checkState,sink,done,input),done);
    }
}
