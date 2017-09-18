package org.kantega.niagara;

import fj.F;
import fj.P2;
import fj.data.Either;
import fj.data.Option;
import fj.function.Effect1;

import java.util.function.Supplier;

import static fj.P.p;

/**
 * Either a exception or a value. Wraps calls to the java api that might fail
 * with an exception.
 *
 * @param <A>
 */
public class Attempt<A> {

    final Either<Throwable, A> value;

    private Attempt(Either<Throwable, A> value) {
        this.value = value;
    }

    public static <A> Attempt<A> value(A a) {
        return new Attempt<>(Either.right(a));
    }

    public static <A> Attempt<A> fail(Throwable t) {
        return new Attempt<>(Either.left(t));
    }

    public static <A, B> F<A, Attempt<B>> tryF(F<A, B> f) {
        return a -> {
            try {
                return Attempt.value(f.f(a));
            } catch (Throwable t) {
                return Attempt.fail(t);
            }
        };
    }

    public static <A> Attempt<A> tryCall(Supplier<A> call) {
        try {
            return Attempt.value(call.get());
        } catch (Throwable t) {
            return Attempt.fail(t);
        }
    }

    public <B> Attempt<B> map(F<A, B> f) {
        return new Attempt<>(value.right().map(f));
    }

    public <B> Attempt<B> bind(F<A, Attempt<B>> f) {
        return fold(Attempt::<B>fail, f::f);
    }


    public <B> Attempt<P2<A, B>> and(Attempt<B> other) {
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

    public A orThrow() throws Exception {
        if (isThrowable()) {
            Throwable t = value.left().value();
            throw new Exception("Attempt failed with " + t.getMessage(), t);
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