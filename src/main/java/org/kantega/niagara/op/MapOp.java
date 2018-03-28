package org.kantega.niagara.op;

import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.MapBlock;

import java.util.function.Function;

public class MapOp<A, B> implements Op<A, B> {

    final Function<A, B> function;

    public MapOp(Function<A, B> function) {
        this.function = function;
    }


    @Override
    public <C> Op<A, C> fuse(Op<B, C> other) {
        if (other instanceof MapOp)
            return new MapOp<>(function.andThen(((MapOp<B, C>) other).function));
        return Op.super.fuse(other);
    }

    @Override
    public Block<A> build(Scope scope, Block<B> block) {
        return new MapBlock<>(function, block);
    }
}
