package org.kantega.niagara.op;

import org.kantega.niagara.sink.Sink;
import org.kantega.niagara.sink.TakeWhileSink;

import java.util.function.Predicate;

public class TakeWhileOp<A> implements KeepTypeOp<A> {

    final Predicate<A> pred;

    public TakeWhileOp(Predicate<A> pred) {
        this.pred = pred;
    }

    @Override
    public Sink<A> build(Sink<A> input) {
        return Sink.sink(
          new TakeWhileSink<>(pred, input.consumer, input.done),
          input.done.comap(this));
    }


}
