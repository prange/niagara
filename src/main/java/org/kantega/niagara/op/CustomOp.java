package org.kantega.niagara.op;

import org.kantega.niagara.sink.Sink;

import java.util.function.Consumer;
import java.util.function.Function;

public class CustomOp<A, B> implements TransformTypeOp<A, B> {

    final Function<Consumer<B>, Consumer<A>> injectFunction;

    public CustomOp(Function<Consumer<B>, Consumer<A>> injectFunction) {
        this.injectFunction = injectFunction;
    }


    @Override
    public Sink<A> build(Sink<B> input) {
        return Sink.sink(injectFunction.apply(input.consumer), input.done.comap(this));
    }
}
