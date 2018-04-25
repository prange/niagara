package org.kantega.niagara;

import fj.Unit;

import java.util.function.Supplier;

/**
 * Represents an evaluation of a value. The value can be eager or lazy. A lazy
 * Eval may
 * be evaluated for every call, or be memoized. It might be computed in another thread
 * but the call is blocking.
 * @param <A> The type of the value that is the result of this evaluation.
 */
public interface Eval<A> {

    Try<A> evaluate();

    default Task<A> toTask(){
        return ()->Eventually.value(evaluate());
    }

    static <A> Eval<A> call(Supplier<A> supplier) {
        return () -> Try.call(supplier);
    }

    static Eval<Unit> effect(Runnable r){
        return ()-> Try.call(()->{
            r.run();
            return Unit.unit();
        });
    }

}
