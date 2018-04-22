package org.kantega.niagara.op;

import org.kantega.niagara.Source;
import org.kantega.niagara.sink.FilteringSink;

import java.util.function.Predicate;

public class FilterOp<A> implements KeepTypeOp<A> {

    final Predicate<A> predicate;

    public FilterOp(Predicate<A> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Source<A> apply0(Source<A> input) {
        return (emit, done) -> input.build(new FilteringSink<>(predicate, emit), done.comap(this));
    }
}
