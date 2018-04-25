package org.kantega.niagara.state;

import org.kantega.niagara.Try;

import java.util.function.Supplier;

public interface Step<A> {

    default Step<A> step() {
        return this;
    }

    Try<A> get();




    default boolean complete() {
        return true;
    }


    static <A> Step<A> trycatch(Supplier<A> st) {
        try {
            return done(st.get());
        } catch (Throwable t) {
            return fail(t);
        }
    }

    static <A> Step<A> fail(Throwable t) {
        return () -> Try.fail(t);
    }

    static <A> Step<A> done(A t) {
        return () -> Try.value(t);
    }

    static <A> Step<A> cont(Supplier<Step<A>> next) {
        return new Step<>() {

            @Override
            public Step<A> step() {
                return next.get();
            }

            @Override
            public Try<A> get() {
                return trampoline(this);
            }

            @Override
            public boolean complete() {
                return false;
            }

        };
    }

    static <A> Try<A> trampoline(final Step<A> trampoline) {
        var step = trampoline;
        while (!step.complete()) {
            try {
                step = step.step();
            } catch (Throwable t) {
                return Try.fail(t);
            }
        }
        return step.get();
    }
}
