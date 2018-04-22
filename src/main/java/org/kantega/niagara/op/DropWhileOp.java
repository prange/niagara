package org.kantega.niagara.op;

import org.kantega.niagara.Source;
import org.kantega.niagara.sink.DropWhileSink;

import java.util.function.Predicate;

public class DropWhileOp<A> implements StageOp<A, A> {

    final Predicate<A> pred;

    public DropWhileOp(Predicate<A> pred) {
        this.pred = pred;
    }

    @Override
    public Source<A> apply0(Source<A> input) {
        return (emit, end) -> input.build(new DropWhileSink<>(pred, input, end), end.comap(this));
    }


}
