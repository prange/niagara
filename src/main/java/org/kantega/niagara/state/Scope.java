package org.kantega.niagara.state;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Scope<O> {

    Consumer<O> sink();

    <T> Step<T> wait(Supplier<Step<T>> next);

    <T> Step<T> resetWait(Supplier<Step<T>> next);

}
