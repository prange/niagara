package org.kantega.niagara.task;

import fj.P2;
import fj.Unit;
import fj.data.Either;
import org.kantega.niagara.Try;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Action<A> {

    enum Tag {
        bind, fail, pure, fork, effect,par
    }


    Tag tag();


    static Action<Unit> run(Runnable r) {
        return new Effect<>(() -> {
            r.run();
            return Unit.unit();
        });
    }

    static <A> Action<A> value(A value) {
        return new Pure<>(value);
    }

    //TODO Fuse alle sync actions on construction
    default <B> Action<B> map(Function<A, B> f) {
        return new Bind<>(
          this,
          aTry -> aTry.fold(Fail::new, a ->
            value(f.apply(a))));
    }

    default <B> Action<B> flatMap(Function<A, Action<B>> f) {
        return new Bind<>(this, aTry -> aTry.fold(Fail::new, f::apply));
    }

    default <B> Action<B> bind(Function<Try<A>, Action<B>> f) {
        return new Bind<>(this, f);
    }

    default <T> T as() {
        return (T) this;
    }

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
              "value=" + value +
              '}';
        }
    }


    class Fork<A> implements Action<Unit> {
        final Action<A> left;
        final Action<A> right;

        public Fork(Action<A> left, Action<A> right) {
            this.left = left;
            this.right = right;
        }


        @Override
        public Tag tag() {
            return Tag.fork;
        }
    }

    class Par<A,B,C> implements Action<C> {
        final Action<A> left;
        final Action<B> right;
        // (Try a, Fiber b) V (Fiber a, Try b) -> Action c
        final Function<Either<P2<Try<A>,Fiber<B>>,P2<Fiber<A>,Try<B>>>,Action<C>> handler;

        public Par(Action<A> left, Action<B> right, Function<Either<P2<Try<A>, Fiber<B>>, P2<Fiber<A>, Try<B>>>, Action<C>> handler) {
            this.left = left;
            this.right = right;
            this.handler = handler;
        }


        @Override
        public Tag tag() {
            return Tag.fork;
        }
    }

    class Effect<A> implements Action<A> {

        final Supplier<A> block;

        public Effect(Supplier<A> block) {
            this.block = block;
        }

        @Override
        public Tag tag() {
            return Tag.effect;
        }
    }
}
