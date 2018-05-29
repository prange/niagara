package org.kantega.niagara.op;

import org.kantega.niagara.state.Scope;

public class NoOp<A> implements KeepTypeOp<A> {

    @Override
    public <C> StageOp<A, C> fuse(StageOp<A, C> other) {
        return other;
    }

    @Override
    public Scope<A> build(Scope<A> input) {
        return input;
    }
}
