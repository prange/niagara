package org.kantega.niagara;

import java.util.function.Supplier;

public interface Eval<A> {

    Attempt<A> evaluate();

    default Task<A> toTask(){
        return ()->Eventually.value(evaluate());
    }

    static <A> Eval<A> call(Supplier<A> supplier) {
        return () -> Attempt.tryCall(supplier);
    }
}
