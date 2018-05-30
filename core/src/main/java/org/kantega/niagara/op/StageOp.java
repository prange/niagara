package org.kantega.niagara.op;

import org.kantega.niagara.Source;
import org.kantega.niagara.state.Scope;

import java.util.function.Function;

public interface StageOp<A, B> extends Function<Source<A>, Source<B>> {

    default <C> StageOp<A, C> fuse(StageOp<B, C> other) {
        return new ChainOp<>(this, other);
    }

    @Override
    default Source<B> apply(Source<A> input) {
        if (input.isNil())
            return Source.nil();
        else
            return
              bSink -> input.build(build(bSink));

    }


    Scope<A> build(Scope<B> next);


}
