package org.kantega.niagara.blocks;

import fj.P2;

import java.util.function.BiFunction;

public class StateBlock<A, S, B> implements Block<A> {

    volatile S memo;
    final Block<P2<S, B>> inner;
    final BiFunction<S, A, P2<S, B>> f;

    public StateBlock(S initState, Block<P2<S, B>> inner, BiFunction<S, A, P2<S, B>> f) {
        this.inner = inner;
        this.f = f;
    }

    @Override
    public void run( A input) {
        P2<S, B> next = f.apply(memo, input);
        memo = next._1();
        inner.run(next);
    }
}
