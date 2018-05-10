package org.kantega.niagara.task;

import fj.P2;
import fj.Unit;
import fj.data.Either;
import org.kantega.niagara.Eval;
import org.kantega.niagara.Try;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.kantega.niagara.task.Util.*;

public interface Task<A> {

    void perform(TaskContext rt, Consumer<Try<A>> continuation);

    static Task<Unit> run(Runnable r) {
        return get(() -> {
            r.run();
            return Unit.unit();
        });
    }

    static <A> Task<A> get(Supplier<A> s) {
        return evaluate(Eval.call(s));
    }

    static <A> Task<A> evaluate(Eval<A> a) {
        return new SyncrEffect<>(a);
    }

    static <A> Task<A> value(A value) {
        return new Pure<>(value);
    }

    static <B> Task<B> fail(Throwable t) {
        return new Fail<>(t);
    }

    static <A, B> Task<Unit> fork(Task<A> aAction, Task<B> bAction) {
        return new Fork<>(aAction, bAction);
    }

    static <A, B, C> Task<C> par(
      Task<A> a,
      Task<B> b,
      Function<Either<P2<Try<A>, Strand<B>>, P2<Strand<A>, Try<B>>>, Task<C>> handler) {
        return new Par<>(a, b, handler);
    }

    static <A, B, C> Task<C> join(Task<A> aAction, Task<B> bAction, BiFunction<A, B, C> joiner) {
        return par(aAction, bAction, p2P2Either ->
          p2P2Either.either(
            leftPair -> {
                var lResult = leftPair._1();
                var rFiber = leftPair._2();
                return lResult.fold(
                  t -> rFiber.interrupt().then(fail(t)),
                  a -> rFiber.attach().map(b -> joiner.apply(a, b))
                );
            },
            rigthPair -> {
                var rResult = rigthPair._2();
                var lFiber = rigthPair._1();
                return rResult.fold(
                  t -> lFiber.interrupt().then(fail(t)),
                  b -> lFiber.attach().map(a -> joiner.apply(a, b))
                );
            }
          ));
    }

    static <A> Task<A> callback(Consumer<Consumer<Try<A>>> handler) {
        var cf = new CompletableFuture<Try<A>>();
        handler.accept(cf::complete);
        return new Callback<>(cf);
    }


    default <B> Task<B> map(Function<A, B> f) {
        return new Bind<>(
          this,
          aTry -> aTry.fold(Fail::new, a ->
            value(f.apply(a))));
    }

    default <B> Task<B> flatMap(Function<A, Task<B>> f) {
        return bind(aTry -> aTry.fold(Fail::new, f::apply));
    }

    default <B> Task<B> then(Task<B> next) {
        return bind(aTry -> aTry.fold(Fail::new, __ -> next));
    }

    default Task<A> handle(Function<Throwable, Task<A>> handler) {
        return bind(aTry -> aTry.fold(handler::apply, Task::value));
    }

    default <B> Task<B> bind(Function<Try<A>, Task<B>> f) {
        return new Bind<>(this, f);
    }

    default Task<A> delay(Duration duration) {
        return new Delayed<>(Either.right(duration), this);
    }

    default Task<A> onFinish(Task<?> cleanup) {
        return new Onfinish<>(this, cleanup);
    }

    // *** Implementations ***

    class Bind<A, B> implements Task<B> {

        final Task<A> action;
        final Function<Try<A>, Task<B>> bindFunction;

        public Bind(Task<A> action, Function<Try<A>, Task<B>> bindFunction) {
            this.action = action;
            this.bindFunction = bindFunction;
        }


        @Override
        public String toString() {
            return "Bind{" +
              "action=" + action +
              ", bindFunction=" + bindFunction +
              '}';
        }

        @Override
        public void perform(TaskContext rt, Consumer<Try<B>> continuation) {
            var cont = bindFunction;
            action.perform(rt,
              aTry -> {
                  if (!rt.isInterrupted() || aTry.isThrowable()) {
                      Task<B> apply = cont.apply(aTry);
                      rt.enqueue(apply, continuation);
                  }
              }
            );

        }
    }

    class Fail<A> implements Task<A> {

        final Throwable t;

        public Fail(Throwable t) {
            this.t = t;
        }


        @Override
        public String toString() {
            return "Fail{" +
              t +
              '}';
        }

        @Override
        public void perform(TaskContext rt, Consumer<Try<A>> continuation) {
            continuation.accept(Try.fail(t));
        }

        @Override
        public <B> Task<B> map(Function<A, B> f) {
            return (Task<B>) this;
        }

        @Override
        public Task<A> handle(Function<Throwable, Task<A>> handler) {
            return handler.apply(t);
        }
    }

    class Pure<A> implements Task<A> {

        final A value;

        public Pure(A value) {
            this.value = value;
        }


        @Override
        public String toString() {
            return "Pure{" +
              value +
              '}';
        }

        @Override
        public void perform(TaskContext rt, Consumer<Try<A>> continuation) {
            continuation.accept(Try.value(value));
        }

        @Override
        public <B> Task<B> map(Function<A, B> f) {
            return value(f.apply(value));
        }
    }


    class Fork<A, B> implements Task<Unit> {
        final Task<A> left;
        final Task<B> right;

        public Fork(Task<A> left, Task<B> right) {
            this.left = left;
            this.right = right;
        }


        @Override
        public String toString() {
            return "Fork{" +
              left +
              ", " + right +
              '}';
        }

        @Override
        public void perform(TaskContext rt, Consumer<Try<Unit>> continuation) {
            rt.enqueue(left, aTry -> {});
            rt.enqueue(right, bTry -> {});
            continuation.accept(Try.value(Unit.unit()));
        }
    }

    class Par<A, B, C> implements Task<C> {
        final Task<A> left;
        final Task<B> right;
        // (Try a, Fiber b) V (Fiber a, Try b) -> Action c
        final Function<Either<P2<Try<A>, Strand<B>>, P2<Strand<A>, Try<B>>>, Task<C>> handler;

        public Par(Task<A> left, Task<B> right, Function<Either<P2<Try<A>, Strand<B>>, P2<Strand<A>, Try<B>>>, Task<C>> handler) {
            this.left = left;
            this.right = right;
            this.handler = handler;
        }


        @Override
        public String toString() {
            return "Par{" +
              left +
              ", " + right +
              ", handler=" + handler +
              '}';
        }

        @Override
        public void perform(TaskContext rt, Consumer<Try<C>> continuation) {
            if (!rt.isInterrupted()) {
                var leftC = new Canceable<>(left);
                var rightC = new Canceable<>(right);
                var gate = new Gate<>(handler, leftC, rightC, rt, continuation);
                P2<TaskContext, TaskContext> branch = rt.branch();
                branch._1().enqueue(leftC, gate::left);
                branch._2().enqueue(rightC, gate::right);
            }
        }
    }

    class SyncrEffect<A> implements Task<A> {

        final Eval<A> block;

        public SyncrEffect(Eval<A> block) {
            this.block = block;
        }


        @Override
        public String toString() {
            return "SyncrEffect{" +
              block +
              '}';
        }

        @Override
        public void perform(TaskContext rt, Consumer<Try<A>> continuation) {
            if (!rt.isInterrupted())
                continuation.accept(block.evaluate());
        }
    }

    class Callback<A> implements Task<A> {

        final CompletableFuture<Try<A>> future;

        public Callback(CompletableFuture<Try<A>> future) {
            this.future = future;
        }


        @Override
        public String toString() {
            return "Callback{" +
              future +
              '}';
        }

        @Override
        public void perform(TaskContext rt, Consumer<Try<A>> continuation) {
            future.thenAccept(aTry -> {
                if (!rt.isInterrupted() || aTry.isThrowable())
                    continuation.accept(aTry);
            });
        }
    }

    class Delayed<A> implements Task<A> {
        final Either<Instant, Duration> instantOrDelay;
        final Task<A> delayedAction;

        public Delayed(Either<Instant, Duration> instantOrDelay, Task<A> delayedAction) {
            this.instantOrDelay = instantOrDelay;
            this.delayedAction = delayedAction;
        }


        @Override
        public String toString() {
            return "Delayed{" +
              instantOrDelay +
              ", " + delayedAction +
              '}';
        }

        @Override
        public void perform(TaskContext rt, Consumer<Try<A>> continuation) {
            rt.schedule(
              delayedAction,
              continuation,
              instantOrDelay.either(i -> Duration.between(Instant.now(), i), d -> d));
        }
    }

    class Canceable<A> implements Task<A>, Strand<A> {

        final Task<A> task;
        final CompletableFuture<Boolean> cancel = new CompletableFuture<>();
        final CompletableFuture<Try<A>> callback = new CompletableFuture<>();

        public Canceable(Task<A> task) {
            this.task = task;
        }

        @Override
        public void perform(TaskContext rt, Consumer<Try<A>> continuation) {
            cancel.thenAccept(t -> rt.interrupt());
            task.perform(rt, aTry -> {
                callback.complete(aTry);
                continuation.accept(aTry);
            });
        }

        @Override
        public Task<Unit> interrupt() {
            var i = new Interrupt(cancel);
            return Task.run(i::interrupt);
        }

        @Override
        public Task<A> attach() {
            return new Task.Callback<>(callback);
        }

        static class Interrupt {
            final CompletableFuture<Boolean> c;

            Interrupt(CompletableFuture<Boolean> c) {
                this.c = c;
            }

            public void interrupt() {
                c.complete(true);
            }

        }
    }

    class Onfinish<A> implements Task<A> {

        final Task<A> wrappedTask;
        final Task<?> cleanupTask;

        public Onfinish(Task<A> wrappedTask, Task<?> cleanupTask) {
            this.wrappedTask = wrappedTask;
            this.cleanupTask = cleanupTask;
        }

        @Override
        public void perform(TaskContext rt, Consumer<Try<A>> continuation) {
            rt.enqueue(wrappedTask, aTry -> {
                rt.enqueue(cleanupTask, unitTry -> {/*ignore output from cleanup*/});
                continuation.accept(aTry);
            });
        }
    }
}
