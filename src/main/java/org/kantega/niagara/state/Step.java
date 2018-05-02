package org.kantega.niagara.state;

import fj.Unit;
import org.kantega.niagara.Emitter;
import org.kantega.niagara.Interrupt;
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

    default <B> Step<B> bind(Function<A, Step<B>> f) {
        return bindTry(aTry -> aTry.fold(Step::fail, f::apply));
    }

    default <B> Step<B> bindTry(Function<Try<A>, Step<B>> f) {
        return new BindStep<>(this, f);
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

    class BindStep<A, B> implements Step<B> {

        Step<A> bound;
        final Function<Try<A>, Step<B>> function;

        public BindStep(Step<A> bound, Function<Try<A>, Step<B>> function) {
            this.bound = bound;
            this.function = function;
        }

        @Override
        public Step<B> step() {
            if (bound.isComplete()) {
                return function.apply(bound.complete());
            }
            bound = bound.step();
            return this;
        }

        @Override
        public Try<B> complete() {
            return function.apply(bound.complete()).complete();
        }

        @Override
        public boolean isComplete() {
            return false;
        }


        @Override
        public <C> Step<C> bindTry(Function<Try<B>, Step<C>> f) {
            return
              new BindStep<>(
                bound,
                aTry ->
                  function.apply(aTry).bindTry(f));
        }
    }

    class EmittingStep<O> implements Step<Unit> {

        final Emitter emitter;
        final Scope<O> loop;
        boolean isWaiting = false;

        EmittingStep(Emitter emitter, Scope<O> loop) {
            this.emitter = emitter;
            this.loop = loop;
        }

        //This is the hotspot, here 99% of all work will be done.
        @Override
        public Step<Unit> step() {
            if (loop.isRunning())
                try {
                    if (emitter.emit()) {
                        if (isWaiting)
                            return loop.resetWait(() -> this);
                        else
                            return this;
                    } else {
                        isWaiting = true;
                        return loop.wait(() -> this);
                    }
                } catch (Throwable t) {
                    return Step.fail(t);
                }
            return Step.done(Unit.unit());
        }

        @Override
        public boolean isComplete() {
            return false;
        }

        @Override
        public Try<Unit> complete() {
            return Step.trampoline(this);
        }

    }

    class InterruptingStep<O> implements Step<Unit> {

        final Scope<O> scope;
        Step<Unit> wrapped;

        public InterruptingStep(Scope<O> scope, Step<Unit> wrapped) {
            this.scope = scope;
            this.wrapped = wrapped;
        }

        @Override
        public Step<Unit> step() {
            if(scope.isRunning())
                wrapped = wrapped.step();
            else
                return done(Unit.unit());
            return this;
        }

        @Override
        public Try<Unit> complete() {
            return wrapped.complete();
        }

        @Override
        public boolean isComplete() {
            return wrapped.isComplete();
        }
    }

}
