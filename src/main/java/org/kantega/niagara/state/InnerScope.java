package org.kantega.niagara.state;

import org.kantega.niagara.Source;
import org.kantega.niagara.op.StageOp;
import org.kantega.niagara.sink.Sink;

import java.util.function.Supplier;

public class InnerScope<O, O2> implements Scope<O2> {

    final StageOp<O, O2> ops;
    final Scope<O> outerScope;
    final Sink<O2> innerSink;

    public InnerScope(StageOp<O, O2> ops, Scope<O> outerScope) {
        this.ops = ops;
        this.outerScope = outerScope;
        Source<O> so =
        Source<O2> so2 = ops.apply(outerScope.sink(),d->{}));
        innerSink = o2 ->
    }

    @Override
    public Sink<O2> sink() {
        return null;
    }

    @Override
    public <T> Step<T> wait(Supplier<Step<T>> next) {
        return null;
    }

    @Override
    public <T> Step<T> resetWait(Supplier<Step<T>> next) {
        return null;
    }
}
