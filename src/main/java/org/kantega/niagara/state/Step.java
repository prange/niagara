package org.kantega.niagara.state;

import java.util.function.Supplier;
import java.util.stream.Stream;

public interface Step<T> {

    default Step<T> step() {
        return this;
    }

    T get();

    default boolean complete() {
        return true;
    }


    static <T> Step<T> done(T t) {
        return () -> t;
    }

    static <T> Step<T> cont(Supplier<Step<T>> next) {
        return new Step<>() {

            @Override
            public Step<T> step() {
                return next.get();
            }

            @Override
            public T get() {
                return trampoline(this);
            }

            @Override
            public boolean complete() {
                return false;
            }

            private T trampoline(final Step<T> trampoline) {
                var step = trampoline;
                while (!step.complete()) {
                    step = step.step();
                }
                return step.get();
            }
        };
    }
}
