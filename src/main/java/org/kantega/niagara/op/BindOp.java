package org.kantega.niagara.op;

import org.kantega.niagara.Plan;
import org.kantega.niagara.sink.BindConsumer;
import org.kantega.niagara.sink.Sink;

import java.util.function.Function;

public class BindOp<O, O2> implements TransformTypeOp<O, O2> {

    final Function<O, Plan<O2>> function;

    public BindOp(Function<O, Plan<O2>> function) {
        this.function = function;
    }

    @Override
    public Sink<O> build(Sink<O2> next) {
        return Sink.sink(new BindConsumer<>(next.consumer, function), next.done.comap(this));
    }
}
