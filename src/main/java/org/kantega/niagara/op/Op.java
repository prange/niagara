package org.kantega.niagara.op;

import org.kantega.niagara.blocks.Block;

public interface Op<A, B> {

    default <C> Op<A, C> fuse(Op<B, C> other) {
        return new AndThenOp<>(this, other);
    }

    Block<A> build(Scope scope,Block<B> block);

}
