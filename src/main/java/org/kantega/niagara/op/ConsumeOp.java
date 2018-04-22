package org.kantega.niagara.op;

import org.kantega.niagara.Source;
import org.kantega.niagara.sink.ConsumerSink;

import java.util.function.Consumer;

public class ConsumeOp<A> implements KeepTypeOp<A> {

    final Consumer<A> consumer;

    public ConsumeOp(Consumer<A> consumer) {
        this.consumer = consumer;
    }


    @Override
    public Source<A> apply0(Source<A> input) {
        return (emit, done) ->
          input.build(new ConsumerSink<>(consumer, emit), done.comap(this));

    }
}
