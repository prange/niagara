package org.kantega.niagara.state;

import org.kantega.niagara.sink.Sink;

import java.util.function.Supplier;

public interface Scope<O> {

    Sink<O> sink();

    <T> Step<T> wait(Supplier<Step<T>> next);


}
