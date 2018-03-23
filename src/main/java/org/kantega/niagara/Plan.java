package org.kantega.niagara;

import fj.P;
import fj.P2;
import fj.Unit;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.op.*;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A Plan is a description of the streaming steps to perform. It is "compiled" into a runnable block by build. Nothing is done
 * before the block is actually run. When the stream ends no traces of the stream exist. The plan has to be compiled and run again if
 * the stream is to be rerun.
 *
 * To extend the stream api you can subtype Plan and add methods that use append to add operational steps to the stream.
 *
 * A plan is immutable. Every call to build creates a new and separate stream. Take caution when pulling data from queues or other
 * data from outside the stream, as they are mutable and change between and during streams.
 * The queeus are not immutable
 * @param <A>
 */
public class Plan<A> {


    private final Op<Unit, A> ops;

    private Plan(Op<Unit, A> ops) {
        this.ops = ops;
    }

    //Create a new plan, containing the operations
    private static <B> Plan<B> plan(Op<Unit, B> ops) {
        return new Plan<>(ops);
    }

    /**
     * Append an operation to the plans operations. The operations are fused toegether if applicable.
     * @param op The operation to append
     * @param <B> The last output type of the resulting stream
     * @return a new plan with the operation appended
     */
    protected <B> Plan<B> append(Op<A, B> op) {
        return plan(ops.fuse(op));
    }

    /**
     * Maps elements in the stream.
     * @param f The mapping function
     * @param <B> the type the stream is mapped to
     * @return a new plan with the mapping operation appended.
     */
    public <B> Plan<B> map(Function<A, B> f) {
        return append(new MapOp<>(f));
    }


    /**
     * Transforms and flattens the output of the transformation.The output is inserted into othe stream in order, and
     * the output is handeles directly without any overhead.
     * @param f The mapping function
     * @param <B> The type the values are mapped into
     * @return a new plan
     */
    public <B> Plan<B> flatMap(Function<A, Iterable<B>> f) {
        return append(new FlatMapOp<>(f));
    }

    /**
     * Transforms elements of the plans into a new stream, and binding them together.
     * The resulting plans have are compiles and run with their own scopes, wich might cause some overhead. If this i a
     * frequest operation consider flatMap instead for high performance applications.
     * @param f the mapping function
     * @param <B> the type of the output
     * @return a new plan
     */
    public <B> Plan<B> bind(Function<A, Plan<B>> f) {
        return append(new BindOp<>(f));
    }

    /**
     * Evaluates a (pissibly sideeffecting) computation in this thread, outputing the result.
     * @param f the evalution
     * @param <B> the result of the evaluation
     * @return a new plan.
     */
    public <B> Plan<B> eval(Function<A, Eval<B>> f) {
        return append(new EvalOp<>(f)); //TODO handle exceptions
    }

    /**
     * Handles elements in the current scope until the predicate yields true, then halts the current scope. If a scope outside the
     * current scopes repeats, the current scope is restarted again.
     * @param pred The predicate that test conditions
     * @return a new plan
     */
    public Plan<A> takeWhile(Predicate<A> pred) {
        return
          append(new TakeWhileOp<>(pred));
    }

    /**
     * Repeats the execution of the current scope indefenately.
     * @return a new plan
     */
    public Plan<A> repeat() {
        return append(new RepeatOp<>(this));
    }

    /**
     * Halts the current scope of the stream on some signal.
     * @param impulse The stopping impulse
     * @return a new plan
     */
    public Plan<A> haltOn(Impulse impulse) {
        return append(new HaltOnOp<>(impulse));
    }

    /**
     * Skips count elements of the current scope.
     * @param count
     * @return a new plan
     */
    public Plan<A> skip(long count) {
        return append(new SkipOp<>(count));
    }

    /**
     * Takes max elements from the current scope, then halts the scope.
     * @param max The max number of elements to emit.
     * @return a new plan
     */
    public Plan<A> take(long max) {
        return append(new TakeOp<>(max));
    }

    /**
     * Zips the stream with a state that is updated and fed into the next execution of this step.
     * @param initState The initial state
     * @param f the mapping function
     * @param <S> the type of the state
     * @param <B> the type of the output
     * @return a new plan
     */
    public <S, B> Plan<P2<S, B>> zipMapWithState(S initState, BiFunction<S, A, P2<S, B>> f) {
        return append(new ZipWithStateOp<>(initState, f));
    }

    /**
     * Zips the stream with the index of the current element
     * @return a new stream
     */
    public Plan<P2<Long, A>> zipWithIndex() {
        return zipMapWithState(0L, (count, a) -> P.p(count + 1, a));
    }

    /**
     * Compiles the steps in this plan into a runnable block, within the provided scope and outputting elements into the
     * provided block.
     * @param scope The scope the streams runs in
     * @param terminator The block that receives data from this stream
     * @return a runnable block.
     */
    public Block<Unit> build(Scope scope, Block<A> terminator) {
        P2<Scope, Block<Unit>> block = ops.build(scope, terminator);
        return block._1().build(block._2());
    }

    /**
     * Compiles the steps of this plan into a runnable block, wrapped into a runnable.
     * Runs in the calling thread.
     * @return
     */
    public Runnable build() {
        return () -> build(Scope.root(), (a) -> {
        }).run(Unit.unit());
    }


    /**
     * Compiles this plan into a runnable block and acumulates the elements that are outputted by the last step. The stream in
     * run when get() is called on the returned supplier.
     * @param initState The inital state to accumulate into.
     * @param f the accumlator
     * @param <S> the type of the acumulated value
     * @return a supplier that runs the stream when get() is called.
     */
    public <S> Supplier<S> buildFold(S initState, BiFunction<S, A, S> f) {
        return () -> {
            AtomicReference<S> state = new AtomicReference<>(initState);
            build(Scope.root(), (a) -> state.updateAndGet(s -> f.apply(s, a))).run(Unit.unit());
            return state.get();
        };
    }
}
