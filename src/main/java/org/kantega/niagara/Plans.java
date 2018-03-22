package org.kantega.niagara;

import org.kantega.niagara.op.IterableOp;

import java.util.Arrays;

public class Plans {

    public static <A> Plan<A> emit(A... vals) {
        return iterable(Arrays.asList(vals));
    }

    public static <A> Plan<A> iterable(Iterable<A> iterable) {
        return Plan.plan(new IterableOp<>(iterable));
    }

}
