package org.kantega.niagara.blocks;

import fj.P2;

import java.util.function.BiFunction;

public class ZipWithStateBlock<S, A, B> implements Block<A> {
    volatile S state;
    final BiFunction<S, A, P2<S, B>> zipFunction;
    final Block<P2<S, B>> next;

    public ZipWithStateBlock(S initState, BiFunction<S, A, P2<S, B>> zipFunction, Block<P2<S, B>> next) {
        this.zipFunction = zipFunction;
        this.state = initState;
        this.next = next;
    }


    @Override
    public void run(A input) {
        P2<S, B> nextState = zipFunction.apply(state, input);
        state = nextState._1();
        next.run(nextState);
    }
}
