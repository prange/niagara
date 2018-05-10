package org.kantega.niagara;

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

    static <A> Eval<A> call(Supplier<A> supplier) {
        return () -> Try.call(supplier);
    }

    static <A> Eval<A> fail(Throwable t){
        return ()->Try.fail(t);
    }

}
