package org.kantega.niagara.op;

import org.kantega.niagara.Source;
import org.kantega.niagara.sink.TakeWhileSink;

import java.util.function.Predicate;

public class TakeWhileOp<A> implements KeepTypeOp<A> {

    final Predicate<A> pred;

    public TakeWhileOp(Predicate<A> pred) {
        this.pred = pred;
    }

    @Override
    public Source<A> apply0(Source<A> input) {
        return (emit, end) -> input.build(
          new TakeWhileSink<>(pred, emit, end),
          end.comap(this));
    }


}
