package org.kantega.niagara.op;

import org.kantega.niagara.state.Scope;

import java.util.function.Consumer;
import java.util.function.Function;

public class CustomOp<A, B> implements TransformTypeOp<A, B> {

    final Function<Consumer<B>, Consumer<A>> injectFunction;

    public CustomOp(Function<Consumer<B>, Consumer<A>> injectFunction) {
        this.injectFunction = injectFunction;
    }


    @Override
    public Scope<A> build(Scope<B> input) {
        return Scope.scope(injectFunction.apply(input.consumer), input.done.comap(this));
    }
}
