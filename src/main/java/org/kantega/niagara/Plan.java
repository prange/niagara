package org.kantega.niagara;

import fj.P;
import fj.P2;
import fj.Unit;
import org.kantega.niagara.op.*;
import org.kantega.niagara.state.Scope;
import org.kantega.niagara.state.Instruction;
import org.kantega.niagara.task.Task;
import org.kantega.niagara.thread.WaitStrategy;

import java.util.Queue;
import java.util.function.*;

/**
 * A Plan is a description of the streaming steps to perform. It is "compiled" into a runnable block by build. Nothing is done
 * before the block is actually run. When the stream ends no traces of the stream exist other than the performed sideeffects.
 * The plan has to be compiled and run again if
 * the stream is to be rerun.
 * <p>
 * To extend the stream api you can subtype Plan and add methods that use append to add operational stages to the source.
 * <p>
 * A plan is immutable. Every call to build creates a new and separate stream. Take caution when pulling data from queues or other
 * data from outside the stream, as they are mutable and change between and during streams.
 * The qeueues for example are not immutable
 *
 * @param <A>
 */
public class Plan<A> {

    public final Instruction<A, Unit> instruction;

    private Plan(Instruction<A, Unit> instruction) {
        this.instruction = instruction;
    }

    /**
     * Creates a new plan with the provided operations
     *
     * @param <A> the type of the value the operation outputs
     * @return a new plan
     */
    public static <A> Plan<A> plan(Instruction<A, Unit> scope) {
        return new Plan<>(scope);
    }

    /**
     * Append an operation to the plans operations. The operations are fused toegether if applicable.
     *
     * @param op  The operation to append
     * @param <B> The last output type of the resulting stream
     * @return a new plan with the operation appended
     */
    protected <B> Plan<B> append(StageOp<A, B> op) {
        return plan(instruction.transform(op));
    }


    /**
     * Append a plan to this plan. The next plan is executed when this plan halts.
     *
     * @param next the next plan to run
     * @return A new plan that first executes this plan, and then executes the next plan.
     */
    public Plan<A> append(Supplier<Plan<A>> next) {
        return plan(instruction.<Unit>append(() -> next.get().instruction));
    }

    /**
     * Joins two plans, attempting to run both at the same time in the same thread. This means that the plans
     * are interleaved, and each plan is stepped in turn. If the source of one plan is empty (for example because its input queue is empty)
     * that plan is skipped for that step.
     *
     * @param other
     * @return
     */
    public Plan<A> join(Plan<A> other) {
        return plan(Instruction.merge(instruction, other.instruction));
    }

    /**
     * Filters the contents of the stream, only keeping the elements for
     * which the predicate holds (alias for keep)
     *
     * @param pred
     * @return
     */
    public Plan<A> filter(Predicate<A> pred) {
        return keep(pred);
    }

    /**
     * Filters the contents of the stream, only keeping the elements for
     * which the predicate holds (alias for keep)
     *
     * @param pred
     * @return
     */
    public Plan<A> keep(Predicate<A> pred) {
        return append(new FilterOp<>(pred));
    }

    /**
     * Filters the contents of the stream, only keeping the elements for
     * which the predicate does not hold (alias for keep)
     *
     * @param pred
     * @return
     */
    public Plan<A> drop(Predicate<A> pred) {
        return keep(pred.negate());
    }

    /**
     * Maps elements in the stream.
     *
     * @param f   The mapping function
     * @param <B> the type the stream is mapped to
     * @return a new plan with the mapping operation appended.
     */
    public <B> Plan<B> map(Function<A, B> f) {
        return append(new MapOp<>(f));
    }


    /**
     * Transforms and flattens the output of the transformation.The output is inserted into othe stream in order, and
     * the output is handled directly without any overhead.
     *
     * @param f   The mapping function
     * @param <B> The type the values are mapped into
     * @return a new plan
     */
    public <B> Plan<B> flatMap(Function<A, Iterable<B>> f) {
        return append(new FlatMapOp<>(f));
    }


    /**
     * Evaluates a (possibly sideeffecting) computation in this thread, outputing the result.
     *
     * @param f   the evalution
     * @param <B> the result of the evaluation
     * @return a new plan.
     */
    public <B> Plan<B> eval(Function<A, Eval<B>> f) {
        return append(new EvalOp<>(f));
    }

    /**
     * Evaluates a (possibly sideeffecting and asynchronous) computation, outputing the result.
     *
     * @param f   the evalution
     * @param <B> the result of the evaluation
     * @return a new plan.
     */
    public <B> Plan<B> evalTask(Function<A, Task<B>> f) {
        return null;
    }

    /**
     * Handles elements in the current scope until the predicate yields true, then halts the current scope. If a scope outside the
     * current scopes repeats, the current scope is restarted again.
     *
     * @param pred The predicate that test conditions
     * @return a new plan
     */
    public Plan<A> takeWhile(Predicate<A> pred) {
        return append(new TakeWhileOp<>(pred));
    }

    /**
     * Ignores output as long as the predicate holds. Start to emit
     * elements as soon as the predicate yields false.
     *
     * @param pred The predicate that tests the output.
     * @return a plan that only outputs elements after the predicate yields false
     */
    public Plan<A> dropWhile(Predicate<A> pred) {
        return append(new TakeWhileOp<>(pred));
    }

    /**
     * Repeats the execution of the current scope until encolsing scope
     * is halted.
     *
     * @return a new plan
     */
    public Plan<A> repeat() {
        return plan(instruction.repeat());
    }

    /**
     * Halts the current scope of the stream on some signal.
     *
     * @param impulse The stop signal
     * @return a new plan
     */
    public Plan<A> haltOn(Interrupt impulse) {
        return append(new TakeWhileOp<A>(a -> impulse.isInterrupted()));
    }

    /**
     * Skips count elements of the current scope.
     *
     * @param count
     * @return a new plan
     */
    public Plan<A> skip(long count) {
        return append(new DropWhileStateOp<>(0L, (sum, msg) -> sum + 1, sum -> sum <= count));
    }

    /**
     * Takes max elements from the current scope, then halts the scope.
     *
     * @param max The max number of elements to emit.
     * @return a new plan
     */
    public Plan<A> take(long max) {
        return append(new TakeWhileStateOp<>(max, (count, a) -> count - 1, count -> count > 0));
    }

    /**
     * Accumulates over the values, emitting the accumulated value for each step.
     *
     * @param initState The initial state
     * @param function  The acumulation function
     * @param <S>       The type of the state
     * @return new new plane with the accumultaion appended to the end.
     */
    public <S> Plan<S> accumulate(S initState, BiFunction<S, A, S> function) {
        return append(new MapWithStateOp<>(initState, function));
    }

    /**
     * Zips the stream with a state that is updated and fed into the next execution of this emit.
     *
     * @param initState The initial state
     * @param f         the mapping function
     * @param <S>       the type of the state
     * @param <B>       the type of the output
     * @return a new plan
     */
    public <S, B> Plan<P2<S, B>> zipMapWithState(S initState, BiFunction<S, A, P2<S, B>> f) {
        return accumulate(P.p(initState, null), (tuple, a) -> f.apply(tuple._1(), a));
    }

    /**
     * Zips the stream with the index of the current element
     *
     * @return a new stream
     */
    public Plan<P2<Long, A>> zipWithIndex() {
        return zipMapWithState(0L, (count, a) -> P.p(count + 1, a));
    }

    /**
     * Emits values to the provided consumer. This operation should not block, because that will block this plan.
     *
     * @param consumer
     * @return
     */
    public Plan<A> to(Consumer<A> consumer) {
        return append(new ConsumeOp<>(consumer));
    }

    /**
     * Offers messages to the provided queue, dropping them if the queue is full
     *
     * @param queue
     * @return
     */
    public Plan<A> offer(Queue<A> queue) {
        return append(new OfferQueueDroppingOp<>(queue));
    }

    /**
     * Offers messages to the provided queue, using the supplied waitstrategy to wait for the queue to free up. (This blocks the
     * operation, and might cause buildups in upstream queues)
     *
     * @param queue the queue to offer values to.
     * @return a new plan with the offer operation appended.
     */
    public Plan<A> offerWait(Queue<A> queue, Supplier<WaitStrategy> waitStrategy) {
        return append(new OfferQueueWaitingOp<>(queue, waitStrategy));
    }


    public Task<Unit> compile() {
        return compile(instruction);
    }

    private static <O, R> Task<Unit> compile(Instruction<O, R> instruction) {
        return (rt, cont) -> {
            try {

                var s = Scope.<O>scope(a -> {}, d -> {});
                var eval = instruction.eval(s);

                eval
                  .perform(rt, eitherTry ->
                    eitherTry.doEffect(
                      t -> cont.accept(Try.fail(t)),
                      either -> {
                          either.left().foreachDoEffect(u -> cont.accept(Try.value(Unit.unit())));
                          either.right().foreachDoEffect(instr -> rt.enqueue(compile(instr), cont));
                      }));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        };
    }

}
