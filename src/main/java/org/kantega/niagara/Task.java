package org.kantega.niagara;

import fj.*;
import fj.control.parallel.Actor;
import fj.control.parallel.Strategy;
import fj.data.Either;
import fj.data.Option;
import fj.function.Effect1;

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
public abstract class Task<A> {


    public static final ExecutorService defaultExecutors =
      Executors.newFixedThreadPool(2);

    public static Strategy<Unit> defaultStrategy =
      Strategy.seqStrategy();


    private Task() {
    }


    /**
     * Creates an Async that is resolved by a callback.
     *
     * @param runner The handler that must execute the task, and eventually call the resolver to resolve
     *               the Async task.
     * @param <A>    The type of the value the task creates asyncronically.
     * @return An async that eventually will produce result.
     */
    public static <A> Task<A> async(TaskBody<A> runner) {
        return new Task<A>() {
            @Override
            public void execute(final Effect1<Attempt<A>> completeHandler) {
                try {
                    runner.run(completeHandler::f);
                } catch (Throwable t) {
                    completeHandler.f(Attempt.fail(t));
                }
            }
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
        return async(aresolver -> aresolver.resolve(Attempt.fail(t)));
    }

    /**
     * Wraps a supplier in a Task
     *
     * @param supplier The upplier that is to be called
     * @param <A>      the type the supplier
     * @return a Task that yields the value of the supplier
     */
    public static <A> Task<A> call(final Supplier<A> supplier) {
        return async(validationResolver -> validationResolver.resolve(Attempt.tryCall(supplier)));
    }


    /**
     * Wraps a callable in a Task
     *
     * @param task The callable to wrap
     * @return Unit
     */
    public static Task<Unit> callVoid(Runnable task) {
        return async(validationResolver -> validationResolver.resolve(Attempt.tryCall(() -> {
            task.run();
            return Unit.unit();
        })));
    }

    /**
     * Puts the argument into a Task.
     */
    public static <A> Task<A> value(final A a) {
        return async(aResolver -> aResolver.resolve(Attempt.value(a)));
    }

    /**
     * Creates an async task that is resolved when both tasks are resolved.
     * The tasks are run in parallell if permitted by the executor.
     */
    public static <A, B> Task<P2<A, B>> and(final Task<A> one, final Task<B> other) {
        return async((Resolver<P2<A, B>> p2Resolver) -> {
            EffectSynchronizer<A, B> effectSynchronizer =
              new EffectSynchronizer<>(p2Resolver);
            one.execute(effectSynchronizer.leftE());
            other.execute(effectSynchronizer.rightE());
        });
    }


    /**
     * Runs the async after the given delay
     */
    public Task<A> delay(Duration duration, final ScheduledExecutorService executorService) {
        return async(completeHandler ->
          executorService.schedule(() -> Task.this.execute(completeHandler::resolve), duration.toMillis(), TimeUnit.MILLISECONDS));
    }


    public Task<A> using(Executor executor) {
        return async(completeHandler ->
          executor.execute(() -> Task.this.execute(completeHandler::resolve)));
    }

    /**
     * Applies f to the result of this Async
     *
     * @param f   the function to apply
     * @param <B> the type the function f produces
     * @return An Async with the result transformed.
     */
    public <B> Task<B> map(F<A, B> f) {
        return flatMap(a -> Task.value(f.f(a)));
    }

    /**
     * Bind the next Aync to this async. If the first async fails the second is not run. If the second fails the result is a fail.
     *
     * @param f   a function that takes the result of this async, and provides the next async.
     * @param <B> the type the next async produces.
     * @return An Async that first executes this task, and then the next task when this task is finished.
     */
    public <B> Task<B> flatMap(F<A, Task<B>> f) {
        return new Task<B>() {
            @Override
            public void execute(Effect1<Attempt<B>> completeHandler) {
                Task.this.execute(task1Attempt -> task1Attempt.fold(Task::<B>fail, f::f).execute(completeHandler));
            }
        };
    }


    public <B> Task<B> mapAttempt(F<Throwable, B> onFail, F<A, B> onValue) {
        return flatMapAttempt(t -> Task.value(onFail.f(t)), a -> Task.value(onValue.f(a)));
    }

    public <B> Task<B> flatMapAttempt(F<Throwable, Task<B>> onFail, F<A, Task<B>> onValue) {
        return new Task<B>() {
            @Override
            public void execute(Effect1<Attempt<B>> completeHandler) {
                Task.this.execute(task1Attempt -> task1Attempt.fold(onFail, onValue).execute(completeHandler));
            }
        };
    }

    public Task<A> onFail(F<Throwable, Task<A>> f) {
        return flatMapAttempt(f, Task::value);
    }

    public <B> Task<B> fold(F<Throwable, B> onFail, F<A, B> onSucc) {
        return mapAttempt(onFail, onSucc);
    }

    /**
     * Run the other Async task after this task completes, disregarding the outcome of the first Async.
     */
    public <B> Task<B> andThen(final Task<B> other) {
        return flatMap(a -> other);
    }

    /**
     * Executes the task and awaits the result for the duration, failing if the result is not awailable within the timeout. Prefer to use the async execute() instead.
     * Uses the default execution strategy
     *
     * @param timeout
     * @return
     */
    public Attempt<A> executeAndAwait(Duration timeout) {
        return executeAndAwait(timeout, defaultStrategy);
    }

    /**
     * Executes the task and awaits the result for the duration, failing if the result is not awailable within the timeout. Prefer to use the async execute() instead
     *
     * @param executionStrategy The parallell execution strategy
     * @param timeout
     * @return
     */
    public Attempt<A> executeAndAwait(Duration timeout, Strategy<Unit> executionStrategy) {
        CountDownLatch latch = new CountDownLatch(1);

        AtomicReference<Attempt<A>> ref = new AtomicReference<>();

        execute(a -> {
            ref.set(a);
            latch.countDown();
        });
        try {
            latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return Attempt.fail(new TimeoutException("The task did not complete within " + timeout.toString()));
        }
        return ref.get();
    }


    /**
     * Executes the task and gets the result. Prefer to use the async execute() instead
     *
     * @return
     */
    public Attempt<A> executeAndGet() {
        return executeAndAwait(Duration.ofMinutes(10));
    }


    public CompletionStage<A> execute() {
        CompletableFuture<A> future = new CompletableFuture<>();
        execute(a -> {
            a.failure().foreachDoEffect(future::completeExceptionally);
            a.toOption().foreachDoEffect(future::complete);
        });
        return future;
    }

    public Task<Unit> toUnit() {
        return map(a -> Unit.unit());
    }

    /**
     * Runs the task using the supplied parallell strategy
     */
    public abstract void execute(Effect1<Attempt<A>> completeHandler);


    /**
     * Interface for tasks that are to be run asyncronusly with a callback to resolve the Async.
     */
    public interface TaskBody<A> {
        void run(Resolver<A> resolver);
    }

    /**
     * Interface for the callback. Resolves the async
     * The resolver passes the result object to the client.
     */
    public interface Resolver<A> {

        /**
         * Passes the result over to the client. The client is run in the same thread as the caller of resolve
         *
         * @param result
         */
        void resolve(Attempt<A> result);

    }


    /*
    Gates two resolvers by calling the final resolver with both arguments when both resolvers have been resolved, possibly in different threads.
     */
    private static class EffectSynchronizer<A, B> {


        final Actor<Either<Attempt<A>, Attempt<B>>> actor;

        volatile Option<Attempt<A>> aValue =
          Option.none();

        volatile Option<Attempt<B>> bValue =
          Option.none();

        EffectSynchronizer(final Resolver<P2<A, B>> targetEffect) {
            //Actor that ensures no sharing of state bewteen threads
            actor =
              Actor.actor(Strategy.seqStrategy(), value -> {
                  if (value.isLeft()) {
                      aValue = Option.some(value.left().value());

                      if (bValue.isSome()) {
                          targetEffect.resolve(aValue.some().and(bValue.some()));
                      }
                  } else {
                      bValue = Option.some(value.right().value());

                      if (aValue.isSome()) {
                          targetEffect.resolve(aValue.some().and(bValue.some()));
                      }
                  }
              });
        }


        private void handle(Either<Attempt<A>, Attempt<B>> value) {
            actor.act(value);
        }

        public Effect1<Attempt<A>> leftE() {
            return a -> handle(Either.left(a));
        }

        public Effect1<Attempt<B>> rightE() {
            return b -> handle(Either.right(b));
        }
    }
}