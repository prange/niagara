package org.kantega.niagara;

import fj.F;
import fj.P;
import fj.P2;
import fj.data.List;
import fj.function.Effect1;

import javax.swing.text.html.Option;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * A computation that eventually yields an A. The equvalent as a Future or Promise.
 * <p>
 * This is literally just a wrapper around a java.util.CompletionStage to align its name
 * to the eventdriven domain, and to provide a more sane api. (The api of CompletionStage
 * is rich, but has really waird naming making it hard to understand and distinguish between the methods)
 *
 * @param <A>
 */
public class Eventually<A> {

    final CompletionStage<A> wrapped;

    public Eventually(CompletionStage<A> wrapped) {
        this.wrapped = wrapped;
    }

    /**
     * Wraps a completion stage
     *
     * @param wrapped The CompletionStage to wrap
     * @param <A>     The type of the value that eventually is produced
     * @return
     */
    public static <A> Eventually<A> wrap(CompletionStage<A> wrapped) {
        return new Eventually<>(wrapped);
    }

    public static <A> Eventually<A> value(A value) {
        return new Eventually<>(CompletableFuture.completedFuture(value));
    }

    public static <A> Eventually<A> never() {
        return new Eventually<>(new CompletableFuture<>());
    }

    public static <A> Eventually<A> async(ExecutorService executorService, Effect1<CompletableFuture<A>> callback) {
        CompletableFuture<A> f = new CompletableFuture<>();
        executorService.execute(() -> callback.f(f));
        return wrap(f);
    }


    public <B> Eventually<B> map(F<A, B> f) {
        return wrap(wrapped.thenApply(f::f));
    }

    public <B> Eventually<B> bind(F<A, Eventually<B>> f) {
        return wrap(wrapped.thenCompose(a -> f.f(a).wrapped));
    }

    public static <A, B> Eventually<P2<A, B>> join(Eventually<A> ea, Eventually<B> eb) {
        return wrap(ea.wrapped.thenCombine(eb.wrapped, P::p));
    }


    public void onComplete(Effect1<Attempt<A>> completeHandler) {
        wrapped.whenComplete((aOrNull, throwableOrNull) -> {
            Attempt<A> result = fj.data.Option.fromNull(aOrNull).map(Attempt::value).orSome(Attempt.fail(throwableOrNull));
            completeHandler.f(result);
        });
    }

    public Eventually<A> handleFail(F<Throwable, A> f) {
        return wrap(wrapped.exceptionally(f::f));
    }

    public <B> Eventually<B> handle(F<Throwable, B> onFail, F<A, B> onSuccess) {
        return wrap(wrapped.handle((aOrNull, throwableOrNull) -> {
            Attempt<A> result = fj.data.Option.fromNull(aOrNull).map(Attempt::value).orSome(Attempt.fail(throwableOrNull));
            return result.fold(onFail, onSuccess);
        }));
    }

    public Attempt<A> await(Duration duration) {
        try {
            return Attempt.value(wrapped.toCompletableFuture().get(duration.toMillis(), TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            return Attempt.fail(e);
        }
    }

    public static <A> Eventually<List<A>> sequenceList(List<Eventually<A>> as) {
        if (as.isEmpty())
            return Eventually.value(List.nil());
        else if (as.isSingle())
            return as.head().map(List::single);
        else {
            return Eventually.join(as.head(), sequenceList(as.tail())).map(pair -> pair._2().cons(pair._1()));
        }
    }

}
