package org.kantega.niagara.state;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class NoWaitScope<O> implements Scope<O> {

    final Consumer<O> consumer;

    public NoWaitScope(Consumer<O> consumer) {
        this.consumer = consumer;
    }

    @Override
    public Consumer<O> sink() {
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
}
