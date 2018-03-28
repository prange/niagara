package org.kantega.niagara.op;

import fj.P;
import fj.P2;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.ZipWithStateBlock;

import java.util.function.BiFunction;

public class ZipWithStateOp<S,A,B> implements Op<A,P2<S,B>> {

    final S initState;
    final BiFunction<S, A, P2<S, B>> zipFunction;

    public ZipWithStateOp(S initState, BiFunction<S, A, P2<S, B>> zipFunction) {
        this.initState = initState;
        this.zipFunction = zipFunction;
    }

    @Override
    public Block<A> build(Scope scope, Block<P2<S,B>> block) {
        return new ZipWithStateBlock<>(initState,zipFunction,block);
    }
}
