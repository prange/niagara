package org.kantega.niagara.op;

import org.kantega.niagara.Source;

import java.util.function.Function;

public interface StageOp<A, B> extends Function<Source<A>, Source<B>> {

    default <C> StageOp<A, C> fuse(StageOp<B, C> other) {
        return new ChainOp<>(this, other);
    }

    default <C> StageOp<A, C> append(StageOp<B, C> other) {
        return new ChainOp<>(this, other);
    }

    @Override
    default Source<B> apply(Source<A> input) {
        if (input.isNil())
            return Source.nil();
        else return apply0(input);
    }

    Source<B> apply0(Source<A> input);


}
