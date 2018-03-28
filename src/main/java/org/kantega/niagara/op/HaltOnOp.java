package org.kantega.niagara.op;

import org.kantega.niagara.Impulse;
import org.kantega.niagara.blocks.Block;

public class HaltOnOp<A, B> implements Op<A, B> {

    final Impulse impulse;
    final Op<A, B> scoped;

    public HaltOnOp(Impulse impulse, Op<A, B> scoped) {
        this.impulse = impulse;
        this.scoped = scoped;
    }

    @Override
    public <C> Op<A, C> fuse(Op<B, C> other) {
        return new HaltOnOp<>(impulse,scoped.fuse(other));
    }

    @Override
    public Block<A> build(Scope scope, Block<B> block) {
        impulse.onImpulse(scope::halt);
        return scoped.build(scope,block);
    }
}
