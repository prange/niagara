package org.kantega.niagara;

import fj.F;
import fj.F2;
import fj.P;
import fj.P2;
import fj.data.Either;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public interface Source<A> {

    enum Result {ack, closed}

    Eventually<Closed> open(Eventually<Stop> stopSignal, SourceListener<A> f);

    /**
     * Creates a task that opens the source when executed. The task resolves when the source is running.
     *
     * @return the task that opens the stream
     */
    default Task<Closed> toTask() {
        CompletableFuture<Stop> closeSignal = new CompletableFuture<>();

        return () -> open(Eventually.wrap(closeSignal), a -> Eventually.value(Result.ack)).map(closedAttempt -> {
            closeSignal.complete(stop);
            return closedAttempt;
        });
    }

    /**
     * Transforms each element the source produces
     *
     * @param f   the transformation function
     * @param <B> the type of the transformation output
     * @return the new transfored stream
     */
    default <B> Source<B> map(F<A, B> f) {
        return (closer, listener) ->
          open(closer, a -> listener.f(f.f(a)));
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

        return (closer, handler) ->
          open(closer, a -> {
              P2<S, B> updated = s.updateAndGet(p2 -> f.f(p2._1(), a));
              handler.f(updated);
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
        return (closer, handler) ->
          open(closer, a -> f.f(a).forEach(handler::handle));
    }

    /**
     * Creates a source of sources, flattening the output.
     *
     * @param f   The transformation function
     * @param <B> the type of the inner source
     * @return a new source
     */
    default <B> Source<B> bind(F<A, Source<B>> f) {
        return (closer, handler) ->
          open(closer, a -> {
              Source<B> b = f.f(a);
              b.open(closer, handler);
          });
    }


    /**
     * Appends the next source when the first one (this) closes.
     *
     * @param next The source that takes over after this one.
     * @return a new Source
     */
    default Source<A> append(Supplier<Source<A>> next) {
        return (closer, handler) ->
          open(closer, handler)
            .bind(closed ->
              next.get().open(closer, handler));

    }

    /**
     * Runs the task when the source closes, ignoring the output from the task.
     *
     * @param cleanup
     * @return
     */
    default Source<A> onClose(Task<?> cleanup) {
        return (closer, handler) ->
          open(closer, handler)
            .bind(closed -> cleanup.execute().map(u -> closed));

    }

    /**
     * Joins two sources together.
     *
     * @param other
     * @return
     */
    default Source<A> join(Source<A> other) {
        return (closer, handler) -> {
            Eventually<Closed> firstRunning  = open(closer, handler);
            Eventually<Closed> secondRunning = other.open(closer, handler);
            return Eventually.join(firstRunning, secondRunning).map(pair -> pair._1());
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
        return (closer, handler) ->
          open(closer, a ->
            task.f(a).execute().onComplete(
              bAttempt -> bAttempt.doEffect(
                t -> t.printStackTrace(),
                b -> handler.handle(b)
              ))
          );
    }


    interface Closed {
    }

    Stop stop = new Stop() {
    };

    interface Stop {
    }

}
