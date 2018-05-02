package org.kantega.niagara.state;

import org.kantega.niagara.sink.Sink;

import java.util.function.Supplier;

public class NoWaitScope<O> implements Scope<O> {

    final Sink<O> consumer;

    public NoWaitScope(Sink<O> consumer) {
        this.consumer = consumer;
    }

    @Override
    public Sink<O> sink() {
        return consumer;
    }

    @Override
    public <T> Step<T> wait(Supplier<Step<T>> next) {
        return next.get();
    }

    @Override
    public <T> Step<T> resetWait(Supplier<Step<T>> next) {
        return next.get();
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public Scope<O> reset() {
        return this;
    }
}
