package org.kantega.niagara.task;

import fj.P2;
import fj.Unit;
import fj.data.Either;
import org.kantega.niagara.Try;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

public interface Action<A> {

    enum Tag {
        bind, fail, pure, fork, effect, par, callback, delayed
    }


    Tag tag();

    //TODO Fuse all sync actions on construction in subclasses

    static Action<Unit> run(Runnable r) {
        return new SyncrEffect<>(() -> {
            r.run();
            return Unit.unit();
        });
    }

    static <A> Action<A> value(A value) {
        return new Pure<>(value);
    }

    static <B> Action<B> fail(Throwable t) {
        return new Fail<>(t);
    }

    static <A, B> Action<Unit> fork(Action<A> aAction, Action<B> bAction) {
        return new Fork<>(aAction, bAction);
    }

    static <A, B, C> Action<C> par(
      Action<A> a,
      Action<B> b,
      Function<Either<P2<Try<A>, Fiber<B>>, P2<Fiber<A>, Try<B>>>, Action<C>> handler) {
        return new Par<>(a, b, handler);
    }

    static <A, B, C> Action<C> join(Action<A> aAction, Action<B> bAction, BiFunction<A, B, C> joiner) {
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

    static <A> Action<A> callback(Consumer<Consumer<Try<A>>> handler) {
        var cf = new CompletableFuture<Try<A>>();
        handler.accept(cf::complete);
        return new Callback<>(cf);
    }


    default <B> Action<B> map(Function<A, B> f) {
        return new Bind<>(
          this,
          aTry -> aTry.fold(Fail::new, a ->
            value(f.apply(a))));
    }

    default <B> Action<B> flatMap(Function<A, Action<B>> f) {
        return bind(aTry -> aTry.fold(Fail::new, f::apply));
    }

    default <B> Action<B> then(Action<B> next) {
        return bind(aTry -> aTry.fold(Fail::new, __ -> next));
    }

    default Action<A> handle(Function<Throwable, Action<A>> handler) {
        return bind(aTry -> aTry.fold(handler::apply, Action::value));
    }

    default <B> Action<B> bind(Function<Try<A>, Action<B>> f) {
        return new Bind<>(this, f);
    }

    default Action<A> delay(Duration duration) {
        return new Delayed<>(Either.right(duration), this);
    }


    // *** Implementations ***

    class Bind<A, B> implements Action<B> {

        final Action<A> action;
        final Function<Try<A>, Action<B>> bindFunction;

        public Bind(Action<A> action, Function<Try<A>, Action<B>> bindFunction) {
            this.action = action;
            this.bindFunction = bindFunction;
        }

        @Override
        public Tag tag() {
            return Tag.bind;
        }

        @Override
        public String toString() {
            return "Bind{" +
              "action=" + action +
              ", bindFunction=" + bindFunction +
              '}';
        }
    }

    class Fail<A> implements Action<A> {

        final Throwable t;

        public Fail(Throwable t) {
            this.t = t;
        }

        @Override
        public Tag tag() {
            return Tag.fail;
        }

        @Override
        public String toString() {
            return "Fail{" +
               t +
              '}';
        }

        @Override
        public <B> Action<B> map(Function<A, B> f) {
            return (Action<B>) this;
        }

        @Override
        public Action<A> handle(Function<Throwable, Action<A>> handler) {
            return handler.apply(t);
        }
    }

    class Pure<A> implements Action<A> {

        final A value;

        public Pure(A value) {
            this.value = value;
        }

        @Override
        public Tag tag() {
            return Tag.pure;
        }

        @Override
        public String toString() {
            return "Pure{" +
               value +
              '}';
        }

        @Override
        public <B> Action<B> map(Function<A, B> f) {
            return value(f.apply(value));
        }
    }


    class Fork<A, B> implements Action<Unit> {
        final Action<A> left;
        final Action<B> right;

        public Fork(Action<A> left, Action<B> right) {
            this.left = left;
            this.right = right;
        }


        @Override
        public Tag tag() {
            return Tag.fork;
        }

        @Override
        public String toString() {
            return "Fork{" +
              left +
              ", " + right +
              '}';
        }
    }

    class Par<A, B, C> implements Action<C> {
        final Action<A> left;
        final Action<B> right;
        // (Try a, Fiber b) V (Fiber a, Try b) -> Action c
        final Function<Either<P2<Try<A>, Fiber<B>>, P2<Fiber<A>, Try<B>>>, Action<C>> handler;

        public Par(Action<A> left, Action<B> right, Function<Either<P2<Try<A>, Fiber<B>>, P2<Fiber<A>, Try<B>>>, Action<C>> handler) {
            this.left = left;
            this.right = right;
            this.handler = handler;
        }


        @Override
        public Tag tag() {
            return Tag.par;
        }

        @Override
        public String toString() {
            return "Par{" +
               left +
              ", " + right +
              ", handler=" + handler +
              '}';
        }
    }

    class SyncrEffect<A> implements Action<A> {

        final Supplier<A> block;

        public SyncrEffect(Supplier<A> block) {
            this.block = block;
        }

        @Override
        public Tag tag() {
            return Tag.effect;
        }

        @Override
        public String toString() {
            return "SyncrEffect{" +
                block +
              '}';
        }
    }

    class Callback<A> implements Action<A> {

        final CompletableFuture<Try<A>> future;

        public Callback(CompletableFuture<Try<A>> future) {
            this.future = future;
        }

        @Override
        public Tag tag() {
            return Tag.callback;
        }

        @Override
        public String toString() {
            return "Callback{" +
               future +
              '}';
        }
    }

    class Delayed<A> implements Action<A> {
        final Either<Instant, Duration> instantOrDelay;
        final Action<A> delayedAction;

        public Delayed(Either<Instant, Duration> instantOrDelay, Action<A> delayedAction) {
            this.instantOrDelay = instantOrDelay;
            this.delayedAction = delayedAction;
        }

        @Override
        public Tag tag() {
            return Tag.delayed;
        }

        @Override
        public String toString() {
            return "Delayed{" +
              instantOrDelay +
              ", " + delayedAction +
              '}';
        }
    }
}
