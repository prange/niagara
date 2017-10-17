package org.kantega.niagara;

import fj.*;
import fj.control.parallel.Actor;
import fj.control.parallel.Strategy;
import fj.data.Either;
import fj.data.List;
import fj.data.Option;
import fj.function.Effect1;
import fj.function.Try0;
import fj.function.TryEffect0;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * The task represents a computation that can be run in an aribitrary thread. A Task object is pure, that is immutable and side effetct
 * free until it is executed. When it is executed, any effects will be run. Tasks that are bound will run in order. Tasks that are joined by and() will
 * be run in parallell if possible. Execution of the Task is deferred to the Strategy that is provided to the task when it is executed.
 * When no Strategy is provided, it will use its defaultStrategy. The default strategy is configurable through the public static variable.
 * <p>
 * There are several ways to create a new Task. The most common one is to use the async() contructor, which takes a TaskBody as an argument.
 * The TaskBody is really just a function run:Resolver->Unit. When run is called, it should execute its computaton, and call resolve() on
 * the resolver when complete.
 */
public interface Task<A> {


    ExecutorService defaultExecutors =
      Executors.newFixedThreadPool(2);

    Strategy<Unit> defaultStrategy =
      Strategy.seqStrategy();


    Task<Unit> noOp =
      Task.value(Unit.unit());

    /**
     * Creates an Async that is resolved by a callback.
     *
     * @param runner The handler that must execute the task, and eventually call the resolver to resolve
     *               the Async task.
     * @param <A>    The type of the value the task creates asyncronically.
     * @return An async that eventually will produce result.
     */
    static <A> Task<A> async(TaskBody<A> runner) {
        return () -> {
            CompletableFuture<A> completableFuture =
              new CompletableFuture<>();

            runner.run(attempt ->
              attempt.doEffect(completableFuture::completeExceptionally, completableFuture::complete));


            return Eventually.wrap(completableFuture);
        };
    }


    /**
     * Creates a Task that fails
     *
     * @param t   The Throwable it fails with
     * @param <A> the type parameter
     * @return a failing Task
     */
    public static <A> Task<A> fail(Throwable t) {
        return async(aresolver -> aresolver.f(Attempt.fail(t)));
    }

    /**
     * Wraps a supplier in a Task
     *
     * @param supplier The upplier that is to be called
     * @param <A>      the type the supplier
     * @return a Task that yields the value of the supplier
     */
    static <A> Task<A> call(final Supplier<A> supplier) {
        return () -> Eventually.wrap(CompletableFuture.completedFuture(supplier.get()));
    }


    /**
     * Wraps a callable in a Task
     *
     * @param task The callable to wrap
     * @return Unit
     */
    static Task<Unit> runnableTask(Runnable task) {
        return async(callback -> {
            task.run();
            callback.f(Attempt.value(Unit.unit()));
        });
    }

    static <A> Task<A> tryTask(Try0<A, ? extends Exception> task) {
        return () -> {
            try {
                return Eventually.value(task.f());
            } catch (Exception e) {
                return Eventually.fail(e);
            }
        };
    }

    static Task<Unit> tryRunnableTask(TryEffect0<? extends Exception> task) {
        return () -> {
            try {
                task.f();
                return Eventually.value(Unit.unit());
            } catch (Exception e) {
                return Eventually.fail(e);
            }
        };
    }

    /**
     * Puts the argument into a Task.
     */
    static <A> Task<A> value(final A a) {
        return () -> Eventually.value(a);
    }



    static <A> Task<List<A>> sequence(List<Task<A>> tasks) {
        if(tasks.isEmpty())
            return Task.value(List.nil());
        else
            return tasks.head().flatMap(a->sequence(tasks.tail()).map(list -> list.cons(a)));
    }


    /**
     * Runs the async after the given delay
     */
    default Task<A> delay(Duration duration, final ScheduledExecutorService executorService) {
        return async(resolver ->
          executorService.schedule(
            () ->
              Task.this.execute().onComplete(resolver::f),
            duration.toMillis(),
            TimeUnit.MILLISECONDS)
        );
    }


    default Task<A> using(Executor executor) {
        return async(completeHandler ->
          executor.execute(() -> Task.this.execute().onComplete(completeHandler)));
    }

    /**
     * Applies f to the result of this Async
     *
     * @param f   the function to apply
     * @param <B> the type the function f produces
     * @return An Async with the result transformed.
     */
    default <B> Task<B> map(F<A, B> f) {
        return flatMap(a -> Task.value(f.f(a)));
    }



    /**
     * Bind the next Aync to this async. If the first async fails the second is not run. If the second fails the result is a fail.
     *
     * @param f   a function that takes the result of this async, and provides the next async.
     * @param <B> the type the next async produces.
     * @return An Async that first executes this task, and then the next task when this task is finished.
     */
    default <B> Task<B> flatMap(F<A, Task<B>> f) {
        return () -> Task.this.execute().bind(a -> f.f(a).execute());
    }

    /**
     * Creates an async task that is resolved when both tasks are resolved.
     * The tasks are run in parallell if permitted by the executor.
     */
    default <B> Task<P2<A, B>> and(final Task<B> other) {
        return () -> Eventually.join(execute(), other.execute());
    }


    default <B> Task<B> mapAttempt(F<Throwable, B> onFail, F<A, B> onValue) {
        return bindAttempt(t -> Task.value(onFail.f(t)), a -> Task.value(onValue.f(a)));
    }

    default <B> Task<B> bindAttempt(F<Throwable, Task<B>> onFail, F<A, Task<B>> onValue) {
        return () -> Task.this.execute().handle(onFail, onValue).bind(Task::execute);
    }

    default Task<A> onFail(F<Throwable, Task<A>> f) {
        return bindAttempt(f, Task::value);
    }

    default <B> Task<B> fold(F<Throwable, B> onFail, F<A, B> onSucc) {
        return mapAttempt(onFail, onSucc);
    }

    /**
     * Run the other Async task after this task completes, disregarding the outcome of the first Async.
     */
    default <B> Task<B> andThen(final Task<B> other) {
        return flatMap(a -> other);
    }

    /**
     * Runs this task, but yields the supplied value instead of the original
     * @param value
     * @param <B>
     * @return
     */
    default <B> Task<B> thenJust(B value){
        return map(x->value);
    }

    /**
     * Executes the task and awaits the result for the duration, failing if the result is not awailable within the timeout. Prefer to use the async execute() instead.
     * Uses the default execution strategy
     *
     * @param timeout
     * @return
     */
    default Attempt<A> executeAndAwait(Duration timeout) {
        return execute().await(timeout);
    }

    /**
     * Executes the task, waits for the result, and folds over it.
     *
     * @param timeout   How long do we want to wait?
     * @param onFail    if the task failed, handle it here
     * @param onSuccess if the task succeeded, handle it here
     * @param <T>       the type of the result
     * @return the rusult.
     */
    default <T> T executeAndFoldResult(Duration timeout, F<Throwable, T> onFail, F<A, T> onSuccess) {
        return executeAndAwait(timeout).fold(onFail, onSuccess);
    }


    default Task<Unit> toUnit() {
        return map(a -> Unit.unit());
    }

    /**
     * Runs the task using the supplied parallell strategy
     */
    Eventually<A> execute();


    /**
     * Interface for tasks that are to be run asyncronusly with a callback to resolve the Async.
     */
    interface TaskBody<A> {
        void run(Effect1<Attempt<A>> resolver);
    }


}