package org.kantega.niagara.op;

import org.kantega.niagara.Source;

public class NoOp<A> implements KeepTypeOp<A> {

    @Override
    public <C> StageOp<A, C> append(StageOp<A, C> other) {
        return other;
    }

    @Override
    public Source<A> apply0(Source<A> input) {
        return input;
    }
}
