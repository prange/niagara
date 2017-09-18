package org.kantega.niagara;

import fj.*;
import fj.data.IterableW;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public interface Source<A> {

    enum Result {ack, closed}

    Eventually<Running> open(SourceListener<A> f);

    default Task<Unit> toTask() {
        return () -> open(a -> Eventually.value(Result.ack)).bind(running -> running.stopped).map(r -> Unit.unit());
    }

    default <B> Source<B> map(F<A, B> f) {
        return handler ->
          open(a -> handler.f(f.f(a)));
    }

    default <S, B> Source<P2<S, B>> zipWithState(S initState, F2<S, A, P2<S, B>> f) {
        AtomicReference<P2<S, B>> s = new AtomicReference<>(P.p(initState, null));

        return handler ->
          open(a -> {
              P2<S, B> updated = s.updateAndGet(p2 -> f.f(p2._1(), a));
              return handler.f(updated);
          });
    }

    default <S> Source<S> foldLeft(S initState, F2<S, A, S> f) {
        return zipWithState(initState, (s, a) -> {
            S next = f.f(s, a);
            return P.p(next, next);
        }).map(P2::_1);
    }

    default <B> Source<B> mapMealy(Mealy<A, B> initMealy) {
        return zipWithState(initMealy, Mealy::apply).map(P2::_2);
    }

    //TODO: Blows up on stack
    default <B> Source<B> flatten(F<A, ? extends Iterable<B>> f) {
        return handler ->
          open(a ->
            IterableW
              .wrap(f.f(a))
              .foldLeft(
                sum -> b -> sum.bind(result -> result == Result.ack ? handler.handle(b) : Eventually.value(result)),
                Eventually.value(Result.ack)));
    }

    default <B> Source<B> bind(F<A, Source<B>> f) {
        return handler ->
          open(a -> {
              Source<B> b = f.f(a);
              return
                b.open(handler)
                  .bind(bRunning -> bRunning.stopped);
          });
    }


    default Source<A> append(Supplier<Source<A>> next) {
        return handler ->
          open(handler)
            .bind(running ->
              running
                .stopped
                .bind(u ->
                  next
                    .get()
                    .open(handler)));

    }

    default Source<A> onClose(Task<Unit> cleanup) {
        return handler ->
          open(handler)
            .map(running -> running.onStop(cleanup));

    }

    /**
     * Failed tasks are ignored!
     *
     * @param task
     * @param <B>
     * @return
     */
    default <B> Source<B> apply(F<A, Task<B>> task) {
        return handler ->
          open(a ->
            task.f(a).execute().bind(handler::handle)
          );
    }

    class Running {

        final Eventually<Result> stopped;


        public Running(Eventually<Result> stopped) {
            this.stopped = stopped;
        }

        public Running onStop(Task<Unit> task) {
            return new Running(stopped.bind(r -> task.execute().map(u -> r)));
        }

    }


}
