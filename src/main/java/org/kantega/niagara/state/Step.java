package org.kantega.niagara.state;

import org.kantega.niagara.Try;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Step<A> {

    default Step<A> step() {
        return this;
    }

    Try<A> complete();

    default boolean isComplete() {
        return true;
    }

    default <B> Step<B> bind(Function<A,Step<B>> f){
        return cont(()->{
            Try<A> aTry = complete();
            return aTry.fold(Step::fail, a->cont(()->f.apply(a)));
        });
    }

    default <B> Step<B> bindTry(Function<Try<A>,Step<B>> f){
        return cont(()->{
            Try<A> aTry = complete();
            return cont(()->f.apply(aTry));
        });
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
            public Try<A> complete() {
                return trampoline(this);
            }

            @Override
            public boolean isComplete() {
                return false;
            }

        };
    }

    static <A> Try<A> trampoline(final Step<A> trampoline) {
        var step = trampoline;
        while (!step.isComplete()) {
            try {
                step = step.step();
            } catch (Throwable t) {
                return Try.fail(t);
            }
        }
        return step.complete();
    }
}
