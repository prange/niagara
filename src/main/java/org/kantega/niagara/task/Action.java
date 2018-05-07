package org.kantega.niagara.task;

import fj.Unit;
import org.kantega.niagara.Try;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Action<A> {

    enum Tag {
        bind, fail, pure, fork, effect
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

    default <B> Action<B> bind(Function<A, Action<B>> f) {
        return new Bind<>(this, aTry -> aTry.fold(Fail::new, f::apply));
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


    class Fork<A> implements Action<A> {
        final Action<A> forked;
        final Optional<Function<Throwable, Action<Unit>>> handler;

        public Fork(Action<A> forked, Optional<Function<Throwable, Action<Unit>>> handler) {
            this.forked = forked;
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
