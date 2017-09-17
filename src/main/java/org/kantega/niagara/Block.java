package org.kantega.niagara;

import fj.*;
import fj.data.IterableW;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public interface Block<A> {

    enum Result {ack, ackAndClosed, closed}

    CompletionStage<Running> open(BlockHandler<A> f);

    default void run() {
        open(a->Result.ack);
    }

    default <B> Block<B> map(F<A, B> f) {
        return handler ->
          open(a -> handler.f(f.f(a)));
    }

    default <S, B> Block<P2<S, B>> mapState(S initState, F2<S, A, P2<S, B>> f) {
        AtomicReference<P2<S, B>> s = new AtomicReference<>(P.p(initState, null));

        return handler ->
          open(a -> {
              P2<S, B> updated = s.updateAndGet(p2 -> f.f(p2._1(), a));
              return handler.f(updated);
          });

    }

    default <B> Block<B> mapMealy(Mealy<A, B> initMealy) {
        return mapState(initMealy, Mealy::apply).map(P2::_2);
    }

    default <B> Block<B> flatten(F<A, ? extends Iterable<B>> f) {
        return handler ->
          open(a ->
            IterableW
              .wrap(f.f(a))
              .foldLeft(result -> b -> result == Result.ack ? handler.f(b) : result, Result.ack));
    }

    default <B> Block<B> bind(F<A, Block<B>> f) {
        return handler ->
          open(a -> {
              Block<B> b = f.f(a);
              Attempt<Result> att =
                await(b.open(handler))
                  .bind(r -> await(r.stopped));
              return att.fold(t -> Result.closed, v -> v);
          });
    }


    default Block<A> append(Supplier<Block<A>> next) {
        return handler ->
          open(handler)
            .thenCompose(running ->
              running
                .stopped
                .thenCompose(u ->
                  next
                    .get()
                    .open(handler)));

    }

    default Block<A> onClose(Task<Unit> cleanup) {
        return handler ->
          open(handler)
            .thenApply(running -> running.onStop(cleanup));

    }

    /**
     * Failed tasks are ignored!
     *
     * @param task
     * @param <B>
     * @return
     */
    default <B> Block<B> apply(F<A, Task<B>> task) {
        AtomicReference<Result> result = new AtomicReference<>(Result.ack);

        return handler ->
          open(a -> {
              task.f(a).execute(att -> att.toOption().foreachDoEffect(b -> {
                  Result r = handler.f(b);
                  result.set(r);
              }));
              return result.get();
          });
    }

    class Running {

        final CompletionStage<Result> stopped;


        public Running(CompletionStage<Result> stopped) {
            this.stopped = stopped;
        }

        public Running onStop(Task<Unit> task) {
            return new Running(stopped.thenCompose(r -> task.execute().thenApply(u -> r)));
        }

    }

    static <A> Attempt<A> await(CompletionStage<A> ca) {
        try {
            return Attempt.value(ca.toCompletableFuture().get(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            return Attempt.fail(e);
        }
    }

}
