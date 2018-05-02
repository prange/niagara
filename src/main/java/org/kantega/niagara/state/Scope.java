package org.kantega.niagara.state;

import org.kantega.niagara.sink.Sink;
import org.kantega.niagara.source.Done;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Scope<O> {

    Sink<O> sink();

    <T> Step<T> wait(Supplier<Step<T>> next);

    <T> Step<T> resetWait(Supplier<Step<T>> next);

    boolean isRunning();

    Scope<O> reset();
}
