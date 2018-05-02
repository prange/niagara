package org.kantega.niagara.state;

import org.kantega.niagara.op.StageOp;
import org.kantega.niagara.sink.Sink;
import org.kantega.niagara.source.Done;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class InnerScope<O, O2> implements Scope<O> {

    final StageOp<O, O2> ops;
    final Scope<O2> outerScope;
    final Sink<O> innerSink;
    boolean isRunning = true;

    public InnerScope(StageOp<O, O2> ops, Scope<O2> outerScope) {
        this.ops = ops;
        this.outerScope = outerScope;
        Sink<O2> outerSink = outerScope.sink();
        innerSink = ops.build(Sink.sink(outerSink.consumer, r -> {
            isRunning = false;
        }));
    }

    static <O> InnerScope<O,O> wrap(Scope<O> outerScope){
        return new InnerScope<>(s->s,outerScope);
    }

    @Override
    public Sink<O> sink() {
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

    @Override
    public boolean isRunning() {
        return isRunning && outerScope.isRunning();
    }

    @Override
    public Scope<O> reset() {
        return new InnerScope<>(ops,outerScope);
    }
}
