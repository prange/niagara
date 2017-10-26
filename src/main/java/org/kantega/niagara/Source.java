package org.kantega.niagara;

import fj.*;
import fj.data.Either;
import fj.data.List;
import fj.data.Option;
import org.kantega.niagara.exchange.Topic;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static fj.P.*;
import static fj.function.Booleans.*;
import static org.kantega.niagara.Task.*;

public interface Source<A> {

    Eventually<Closed> open(Eventually<Stop> stopSignal, SourceListener<A> f);

    default <B> Source<B> wrap(F<SourceListener<B>, SourceListener<A>> f) {
        return (closer, handler) -> open(closer, f.f(handler));
    }

    default Source<A> closeOn(Eventually<Stop> stopSignal) {
        return (closer, handler) -> open(closer.or(stopSignal), handler);
    }

    /**
     * Creates a task that opens the source when executed. The task resolves when the source is running.
     *
     * @return the task that opens the stream
     */
    default Task<Closed> toTask() {
        CompletableFuture<Stop> closeSignal = new CompletableFuture<>();

        return () ->
          open(
            Eventually.wrap(closeSignal),
            a -> noOp
          )
            .map(closedAttempt -> {
                closeSignal.complete(stop);
                return closedAttempt;
            });
    }

    default Source<A> to(Sink<A> sink) {
        return apply(a -> sink.consume(a).map(u -> a));
    }

    /**
     * Transforms each element the source produces
     *
     * @param f   the transformation function
     * @param <B> the type of the transformation output
     * @return the new transfored stream
     */
    default <B> Source<B> map(F<A, B> f) {
        return wrap(listener -> (a -> listener.handle(f.f(a))));
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
        AtomicReference<P2<S, B>> s = new AtomicReference<>(p(initState, null));

        return
          wrap(handler -> a -> {
              P2<S, B> updated = s.updateAndGet(p2 -> f.f(p2._1(), a));
              return handler.handle(updated);
          });
    }


    default <S, B> Source<B> mapWithState(S state, F2<S, A, P2<S, B>> f) {
        return
          zipWithState(state, f).map(P2::_2);
    }


    default Source<P2<Long, A>> zipWithIndex() {
        return
          zipWithState(0L, (sum, val) -> p(sum + 1, val));
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
            return p(next, next);
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
    default <B> Source<B> mapWithMealy(Mealy<A, B> initMealy) {
        return zipWithState(initMealy, (s, a) -> s.apply(a).toTuple()).map(P2::_2);
    }


    /**
     * Creates a source where the output of the transformation
     * is pushed one by one
     *
     * @param f   the transformation
     * @param <B> the type of the values in the iterable
     * @return a new flattened source.
     */
    default <B> Source<B> flatten(F<A, Iterable<B>> f) {
        return
          wrap(handler -> a -> sequence(List.iterableList(f.f(a)).map(handler::handle)).toUnit());
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
              return () -> b.open(closer, handler).map(u -> Unit.unit());
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
     * The source ends when both of the joined sources have ended
     *
     * @param other
     * @return
     */
    default Source<A> join(Source<A> other) {
        return (closer, handler) -> {
            Eventually<Closed> firstRunning  = open(closer, handler);
            Eventually<Closed> secondRunning = other.open(closer, handler);
            return Eventually.join(firstRunning, secondRunning).map(P2::_1);
        };
    }

    /**
     * Send the values of this source through the stream.
     *
     * @param f
     * @param <B>
     * @return
     */
    default <B> Source<B> through(Stream<A, B> f) {
        return f.apply(this);
    }

    default <B> Source<Either<A, B>> or(Source<B> other) {
        return this.<Either<A, B>>map(Either::left).join(other.map(Either::right));

    }

    default Source<A> keep(F<A, Boolean> predicate) {
        return keepSomes(a -> predicate.f(a) ? Option.some(a) : Option.none());
    }

    default Source<A> drop(F<A, Boolean> predicate) {
        return keep(not(predicate));
    }

    default <B> Source<B> keepSomes(F<A, Option<B>> toOption) {
        return map(toOption).flatten(o -> o);
    }


    default <B> Source<B> split(
      Stream<A, B> leftStream,
      Stream<A, B> rightStream) {


        return (closer, handler) -> {
            Topic<A> aTopic = new Topic<>();
            Topic<B> bTopic = new Topic<>();

            Task<Closed> los =
              leftStream.apply(aTopic.subscribe()).apply(bTopic::publish).closeOn(closer).toTask();

            Task<Closed> ros =
              rightStream.apply(aTopic.subscribe()).apply(bTopic::publish).closeOn(closer).toTask();

            Task<Closed> tos =
              Source.this.apply(aTopic::publish).toTask();

            Task<Closed> bos =
              bTopic.subscribe().apply(handler::handle).toTask();

            return los.and(ros).and(tos).and(bos).thenJust(Source.stopped()).execute();
        };
    }

    /**
     * Failed tasks halt the source!
     *
     * @param task
     * @param <B>
     * @return
     */
    default <B> Source<B> apply(F<A, Task<B>> task) {
        return (closer, handler) -> {
            CompletableFuture<Closed> failCloser =
              new CompletableFuture<>();

            return open(closer, a ->
              task
                .f(a)
                .flatMap(handler::handle)
                .onFail(t -> runnableTask(() -> failCloser.completeExceptionally(t)))
            ).or(Eventually.wrap(failCloser));
        };
    }


    default Source<A> take(long max) {
        return zipWithIndex().until(pair -> pair._1() > max).map(P2::_2);
    }

    default Source<A> skip(long max) {
        return zipWithIndex().drop(pair -> pair._1() < max).map(P2::_2);
    }


    default Source<A> asLongAs(F<A, Boolean> pred) {
        return until(not(pred));
    }

    default Source<A> until(F<A, Boolean> predicate) {
        CompletableFuture<Closed> closed = new CompletableFuture<>();

        return (closer, handler) -> {
            Eventually<Closed> innerStopped =
              open(closer, a ->
                predicate.f(a) ?
                  runnableTask(() -> closed.complete(ended())) :
                  handler.handle(a));

            return Eventually.firstOf(innerStopped, Eventually.wrap(closed));
        };
    }

    default Source<P2<A, A>> window2() {
        Source<P2<Option<A>, Option<A>>> pairs =
          zipWithState(Option.none(), (maybeFirst, second) -> p(Option.some(second), maybeFirst));
        return pairs.keepSomes(pair -> pair._1().bind(first -> pair._2().map(second -> p(first, second))));
    }

    default Source<A> changes(Equal<A> eq) {
        return compareKeep(eq.eq());
    }

    default Source<A> compareKeep(F2<A, A, Boolean> compare) {
        return window2().keep(F2Functions.tuple(compare)).map(P2.__2());
    }


    static Closed stopped() {
        return closed("stopped");
    }

    static Closed ended() {
        return closed("Ended");
    }

    static Closed closed(String reason) {
        return new Closed(reason, Option.none());
    }

    static Closed closed(String reason, Throwable e) {
        return new Closed(reason, Option.some(e));
    }

    class Closed {
        final String            reason;
        final Option<Throwable> t;

        public Closed(String reason, Option<Throwable> t) {
            this.reason = reason;
            this.t = t;
        }

        @Override
        public String toString() {
            return "Closed{" + "reason='" + reason + '\'' +
              ", t=" + t +
              '}';
        }
    }

    Stop stop = new Stop() {
    };

    interface Stop {
    }

}
