package org.kantega.niagara;

import java.util.function.Function;

/**
 * Transformation of a stream
 * @param <I>
 * @param <O>
 */
public interface Pipe<I, O> extends Function<Plan<I>, Plan<O>> {

    Plan<O> apply(Plan<I> input);

    default <O2> Pipe<I, O2> appendOp(Function<Plan<O>, Plan<O2>> f) {
        return input -> f.apply(apply(input));
    }

    default <O2> Pipe<I, O2> pipe(Pipe<O, O2> other) {
        return input -> other.apply(apply(input));
    }

    default <B> Pipe<I, B> map(Function<O, B> f) {
        return appendOp(s -> s.map(f));
    }

}
