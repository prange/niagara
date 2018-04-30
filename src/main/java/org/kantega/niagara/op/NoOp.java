package org.kantega.niagara.op;

import org.kantega.niagara.sink.Sink;

public class NoOp<A> implements KeepTypeOp<A> {

    @Override
    public <C> StageOp<A, C> fuse(StageOp<A, C> other) {
        return other;
    }

    @Override
    public Sink<A> build(Sink<A> input) {
        return input;
    }
}
