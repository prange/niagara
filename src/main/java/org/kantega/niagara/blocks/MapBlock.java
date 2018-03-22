package org.kantega.niagara.blocks;

import java.util.function.Function;

public class MapBlock<A, B> implements Block<A> {

    final Block<B> inner;
    final Function<A, B> f;

    public MapBlock(Function<A, B> f, Block<B> inner) {
        this.inner = inner;
        this.f = f;
    }

    @Override
    public void run(A input) {
        inner.run(f.apply(input));
    }
}
