package org.kantega.niagara.state;

import org.kantega.niagara.op.StageOp;
import org.kantega.niagara.sink.Sink;
import org.kantega.niagara.source.Done;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class InnerScope<O, O2> implements Scope<O> {

    final StageOp<O, O2> ops;
    final Scope<O2> outerScope;
    final Consumer<O> innerSink;

    public InnerScope(StageOp<O, O2> ops, Scope<O2> outerScope) {
        this.ops = ops;
        this.outerScope = outerScope;
        innerSink = ops.build(Sink.sink(outerScope.sink(),Done.noOp())).consumer;
    }

    @Override
    public Consumer<O> sink() {
        return innerSink;
    }

    @Override
    public <T> Step<T> wait(Supplier<Step<T>> next) {
        return outerScope.wait(next);
    }

    @Override
    public <T> Step<T> resetWait(Supplier<Step<T>> next) {
        return outerScope.resetWait(next);
    }
}
