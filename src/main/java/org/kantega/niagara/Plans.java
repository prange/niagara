package org.kantega.niagara;

import org.kantega.niagara.concurrent.WaitStrategy;
import org.kantega.niagara.op.IterableOp;
import org.kantega.niagara.op.PollOp;
import org.kantega.niagara.op.PullRepeatOp;

import java.util.Arrays;
import java.util.Queue;
import java.util.function.Supplier;

public class Plans {

    public static <A> Plan<A> emit(A... vals) {
        return iterable(Arrays.asList(vals));
    }

    public static <A> Plan<A> iterable(Iterable<A> iterable) {
        return Plan.plan(new IterableOp<>(iterable));
    }

    public static <A> Plan<A> pullForever(Queue<A> supplier, Supplier<WaitStrategy> waitStrategy, Supplier<WaitStrategy> repeatStrategy) {
        return Plan.plan(new PullRepeatOp<>(supplier, waitStrategy, repeatStrategy));
    }

    public static <A> Plan<A> pullAwaitSingle(Queue<A> supplier, Supplier<WaitStrategy> waitStrategy, Supplier<WaitStrategy> repeatStrategy) {
        return Plan.plan(new PullRepeatOp<>(supplier, waitStrategy, repeatStrategy));
    }

    public static <A> Plan<A> poll(Queue<A> supplier) {
        return Plan.plan(new PollOp<>(supplier));
    }

}
