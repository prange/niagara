package org.kantega.niagara;

import fj.F;
import fj.P2;
import fj.Unit;
import fj.data.Either;
import fj.data.Option;
import fj.function.Effect1;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static fj.P.p;

/**
 * Either a exception or a value. Wraps calls to the java api that might fail
 * with an exception.
 *
 * @param <A>
 */
public class Try<A> {

    final Either<Throwable, A> value;

    private Try(Either<Throwable, A> value) {
        this.value = value;
    }

    public static <A> Try<A> value(A a) {
        return new Try<>(Either.right(a));
    }

    public static <A> Try<A> fail(Throwable t) {
        return new Try<>(Either.left(t));
    }

    public static <A, B> F<A, Try<B>> lift(Function<A, B> f) {
        return a -> {
            try {
                return Try.value(f.apply(a));
            } catch (Throwable t) {
                return Try.fail(t);
            }
        };
    }

    public static <A> Try<A> call(Supplier<A> call) {
        try {
            return Try.value(call.get());
        } catch (Throwable t) {
            return Try.fail(t);
        }
    }

    public static Try<Unit> call(Runnable r) {
        try {
            r.run();
            return Try.value(Unit.unit());
        } catch (Throwable t) {
            return Try.fail(t);
        }
    }

    public static <A> Consumer<Try<A>> attemptHandler(Effect1<Throwable> onThrowable, Effect1<A> onA) {
        return attempt -> {
            attempt.toOption().foreachDoEffect(onA);
            attempt.failure().foreachDoEffect(onThrowable);
        };
    }

    public CompletableFuture<A> toCompletedFuture() {
        CompletableFuture<A> cf = new CompletableFuture<>();
        doEffect(cf::completeExceptionally, cf::complete);
        return cf;
    }

    public <B> Try<B> map(Function<A, B> f) {
        return new Try<>(value.right().map(f::apply));
    }

    public <B> Try<B> bind(Function<A, Try<B>> f) {
        return fold(Try::<B>fail, f::apply);
    }


    public <B> Try<P2<A, B>> and(Try<B> other) {
        return bind(valueA -> other.map(valueB -> (p(valueA, valueB))));
    }

    public boolean isValue() {
        return value.isRight();
    }

    public boolean isThrowable() {
        return value.isLeft();
    }

    public Option<A> toOption() {
        return value.right().toOption();
    }

    public Option<Throwable> failure() {
        return value.left().toOption();
    }

    public A orThrow() {
        if (isThrowable()) {
            Throwable t = value.left().value();
            throw new RuntimeException("Attempt failed with " + t.getMessage(), t);
        } else
            return value.right().value();
    }

    public <X> X fold(F<Throwable, X> g, F<A, X> f) {
        return value.either(g, f);
    }

    public void doEffect(Effect1<Throwable> onThrowable, Effect1<A> onValue) {
        Runnable r =
          fold(f -> () -> onThrowable.f(f), v -> () -> onValue.f(v));

        r.run();

    }

}