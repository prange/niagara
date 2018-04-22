package org.kantega.niagara;

import org.kantega.niagara.source.IterableSource;
import org.kantega.niagara.source.NilSource;
import org.kantega.niagara.source.QueueSource;
import org.kantega.niagara.source.SingleValueSource;
import org.kantega.niagara.state.Instruction;

import java.util.Arrays;
import java.util.Queue;

import static org.kantega.niagara.Plan.plan;

/**
 * A selection of starting points for creating plans
 */
public class Plans {


    static <O> Plan<O> emit(O... values) {
        return
          values.length == 0 ?
            Plans.nil() :
            iterable(Arrays.asList(values));
    }


    static <O> Plan<O> nil() {
        return plan(Instruction.source(new NilSource<>()));
    }


    /**
     * Emits the value provided.
     *
     * @param value the values to emit
     * @param <A>   The type of the values
     * @return a plan that emits the values provided
     */
    public static <A> Plan<A> single(A value) {
        return plan(Instruction.source(new SingleValueSource<>(value)));
    }

    /**
     * Emits the values in the iterable.
     *
     * @param iterable the values to emit
     * @param <A>      The type of the values
     * @return a plan that emits the values provided
     */
    public static <A> Plan<A> iterable(Iterable<A> iterable) {
        return plan(Instruction.source(new IterableSource<>(iterable)));
    }


    /**
     * Polls a queue, doing nothing if the queue is empty.
     *
     * @param q   The queue
     * @param <A> the type of the element
     * @return a  plan that polls a queue, and does nothing if the queue is empty.
     */
    public static <A> Plan<A> poll(Queue<A> q) {
        return plan(Instruction.source(new QueueSource<>(q)));
    }

}
