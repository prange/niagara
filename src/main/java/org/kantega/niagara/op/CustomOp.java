package org.kantega.niagara.op;

import org.kantega.niagara.Source;
import org.kantega.niagara.sink.Sink;

import java.util.function.Function;

public class CustomOp<A, B> implements TransformTypeOp<A, B> {

    final Function<Sink<B>, Sink<A>> injectFunction;

    public CustomOp(Function<Sink<B>, Sink<A>> injectFunction) {
        this.injectFunction = injectFunction;
    }


    @Override
    public Source<B> apply0(Source<A> input) {
        return (sink, done) -> input.build(injectFunction.apply(sink), done.comap(this));
    }
}
