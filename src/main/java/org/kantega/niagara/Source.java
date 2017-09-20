package org.kantega.niagara;

import fj.*;
import fj.data.Either;
import fj.data.IterableW;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public interface Source<A> {

    enum Result {ack, closed}

    Eventually<Running> open(SourceListener<A> f);

    /**
     * Creates a task that opens the source when executed. The task resolves when the source closes.
     *
     * @return the task that opens the stream
     */
    default Task<Unit> toTask() {
        return () -> open(a -> Eventually.value(Result.ack)).bind(running -> running.stopped).map(r -> Unit.unit());
    }

    /**
     * Transforms each element the source produces
     *
     * @param f   the transformation function
     * @param <B> the type of the transformation output
     * @return the new transfored stream
     */
    default <B> Source<B> map(F<A, B> f) {
        return handler ->
          open(a -> handler.f(f.f(a)));
    }

    /**
     * Maps over a Moore machine. The state S is kept, and also pushed down
     * with the output of the machine.
     *
     * @param initState the initial state
     * @param f         the transformation function
     * @param <S>       the type of the state
     * @return the transformed source
     */
    default <S, B> Source<P2<S, B>> zipWithState(S initState, F2<S, A, P2<S, B>> f) {
        AtomicReference<P2<S, B>> s = new AtomicReference<>(P.p(initState, null));

        return handler ->
          open(a -> {
              P2<S, B> updated = s.updateAndGet(p2 -> f.f(p2._1(), a));
              return handler.f(updated);
          });
    }

    /**
     * Maps over a Moore machine. The state S is kept, but not pushed
     * with the output of the machine.
     *
     * @param initState the initial state
     * @param f         the transformation function
     * @param <S>       the type of the state
     * @return the transformed source
     */
    default <S> Source<S> foldLeft(S initState, F2<S, A, S> f) {
        return zipWithState(initState, (s, a) -> {
            S next = f.f(s, a);
            return P.p(next, next);
        }).map(P2::_1);
    }

    /**
     * Maps over a Mealy machine. The next mealy is kept, the value
     * is pushed to the handler.
     *
     * @param initMealy The initial mealy
     * @param <B>       The type of the value the mealy outputs
     * @return the transformed stream
     */
    default <B> Source<B> mapMealy(Mealy<A, B> initMealy) {
        return zipWithState(initMealy, Mealy::apply).map(P2::_2);
    }

    /**
     * Creates a source where the output of the transformation
     * is pushed one by one
     *
     * @param f   the transformation
     * @param <B> the type of the values in the iterable
     * @return a new flattened source.
     */
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

    /**
     * Creates a source of sources, flattening the output.
     *
     * @param f   The transformation function
     * @param <B> the type of the inner source
     * @return a new source
     */
    default <B> Source<B> bind(F<A, Source<B>> f) {
        return handler ->
          open(a -> {
              Source<B> b = f.f(a);
              return
                b.open(handler)
                  .bind(bRunning -> bRunning.stopped);
          });
    }


    /**
     * Appends the next source when the first one (this) closes.
     *
     * @param next The source that takes over after this one.
     * @return a new Source
     */
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

    /**
     * Runs the task when the source closes, ignoring the output from the task.
     *
     * @param cleanup
     * @return
     */
    default Source<A> onClose(Task<?> cleanup) {
        return handler ->
          open(handler)
            .map(running -> running.onStop(cleanup));

    }

    /**
     * Joins two sources together.
     *
     * @param other
     * @return
     */
    default Source<A> join(Source<A> other) {
        return handler -> {
            Eventually<Running> firstRunning  = open(handler);
            Eventually<Running> secondRunning = other.open(handler);
            return Eventually.join(firstRunning, secondRunning).map(pair -> new Running(Eventually.join(pair._1().stopped, pair._2().stopped).map(resultPair ->
              resultPair._1().equals(Result.closed) && resultPair._2().equals(Result.closed) ? Result.closed : Result.ack)));
        };
    }

    /**
     * Use update to make changes to the stream in a chaining fashion
     * instead of a nested fashion when applying transformations that are
     * not in the api. Keeps the code tidier.
     *
     * @param f
     * @param <B>
     * @return
     */
    default <B> Source<B> update(F<Source<A>, Source<B>> f) {
        return f.f(this);
    }

    default <B> Source<Either<A, B>> or(Source<B> other) {
        return this.<Either<A, B>>map(Either::left).join(other.map(Either::right));

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

    /**
     * Represents the running state of the source, and wraps an eventual result that
     * is resolved when the source is closed. The result is usually the last
     * reply the source had from its handler when it closed. (But this is not enforces through
     * the typesystem, so beware of programmer errors here)
     */
    class Running {

        final Eventually<Result> stopped;
        final CompletableFuture<Result> stopper = new CompletableFuture<>();

        public Running(Eventually<Result> stopped) {
            this.stopped = Eventually.wrap(stopped.wrapped.applyToEither(stopper,l->l));
        }

        public Running onStop(Task<?> task) {
            return new Running(stopped.bind(r -> task.execute().map(u -> r)));
        }

        public Eventually<Result> stop() {
            stopper.complete(Result.closed);
            return stopped;
        }
    }


}
