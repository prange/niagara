package org.kantega.niagara;

import fj.F;
import fj.F2;
import fj.P;
import fj.P2;
import fj.data.Option;
import fj.function.Effect1;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static fj.data.Option.*;
import static fj.data.Option.some;
import static org.kantega.niagara.Eventually.firstOf;
import static org.kantega.niagara.Eventually.value;

public interface Stream<A> {


    Eventually<Next<A>> open(CompletionStage<Stop> stop);

    default Task<End<?>> run() {
        return () -> {
            AttemptHandler<A> handleAttempt = new AttemptHandler<>();
            open(handleAttempt.stopper).onComplete(handleAttempt::handleAttempt);
            return Eventually.wrap(handleAttempt.taskCompletionFuture);
        };
    }

    class AttemptHandler<A> {

        final CompletableFuture<End<?>> taskCompletionFuture = new CompletableFuture<>();
        final CompletableFuture<Stop>   stopper              = new CompletableFuture<>();

        public void handleAttempt(Attempt<Next<A>> attempt) {
            Attempt.attemptHandler(
              t -> stopper.complete(stop),
              (Next<A> next) -> next.foldEffect(cont -> cont.next.get().open(stopper).onComplete(this::handleAttempt), taskCompletionFuture::complete)
            ).f(attempt);
        }
    }

    default <B> Stream<B> transform(F<Next<A>, Next<B>> next) {
        return (stop) -> open(stop).map(next);
    }

    default <B> Stream<B> transformAsync(F<Next<A>, Eventually<Next<B>>> next) {
        return (stop) -> open(stop).bind(next);
    }

    default <B> Stream<B> fold(F<Continue<A>, Next<B>> onCont, F<End<A>, Next<B>> onEnd) {
        return transform(next -> next.fold(onCont, onEnd));
    }

    default <B> Stream<B> foldAsync(F<Continue<A>, Eventually<Next<B>>> onCont, F<End<A>, Eventually<Next<B>>> onEnd) {
        return transformAsync(next -> next.fold(onCont, onEnd));
    }

    default <B> Stream<B> onCont(F<Continue<A>, Next<B>> onCont) {
        return fold(onCont, e -> end());
    }

    default <B> Stream<B> onContAsync(F<Continue<A>, Eventually<Next<B>>> onCont) {
        return foldAsync(onCont, e -> value(end()));
    }

    default <B> Stream<B> map(F<A, B> f) {
        return onCont(c -> cont(f.f(c.value), ()->c.next.get().map(f)));
    }

    default Stream<A> append(Supplier<Stream<A>> next) {
        return (stop) -> transformAsync(n -> n.fold(Eventually::value, e -> next.get().open(stop))).open(stop);
    }

    default Stream<A> onClose(Task<?> task) {
        return transformAsync(n -> n.fold(Eventually::value, e -> task.execute().map(u -> e)));
    }

    default <B> Stream<B> apply(F<A, Task<B>> tasks) {
        return onContAsync(aContinue -> tasks.f(aContinue.value).execute().map(b -> cont(b, ()->aContinue.next.get().apply(tasks))));
    }

    default <B> Stream<B> bind(F<A, Stream<B>> f) {
        return
          (stop) -> onContAsync(
            c -> {
                Stream<B> bStream = f.f(c.value).append(() -> c.next.get().bind(f));
                return bStream.open(stop);
            }).open(stop);
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
    default <S, B> Stream<P2<S, B>> zipWithState(S initState, F2<S, A, P2<S, B>> f) {
        AtomicReference<P2<S, B>> s = new AtomicReference<>(P.p(initState, null));

        return
          onCont(cont -> {
              P2<S, B> updated = s.updateAndGet(p2 -> f.f(p2._1(), cont.value));
              return cont(updated, ()->cont.next.get().zipWithState(updated._1(), f));
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
    default <S> Stream<S> foldLeft(S initState, F2<S, A, S> f) {
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
    default <B> Stream<B> mapMealy(Mealy<A, B> initMealy) {
        return zipWithState(initMealy, Mealy::apply).map(P2::_2);
    }


    default Stream<A> join(Stream<A> other) {
        return (stop) -> {
            Eventually<Next<A>> one = open(stop);
            Eventually<Next<A>> two = other.open(stop);

            Eventually<Next<A>> first = firstOf(one, two);

            return first.map(firstNext ->
              firstNext.fold(
                cont -> {
                    try {
                        if (one.wrapped.toCompletableFuture().isDone() && one.wrapped.toCompletableFuture().get() == cont) {
                            return cont(cont.value, ()->Streams.wrap(two).join(cont.next.get()));
                        } else {
                            return cont(cont.value, ()->Streams.wrap(one).join(cont.next.get()));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                end -> end()
              )
            );
        };
    }

    abstract class Next<A> {

        abstract public <T> T fold(F<Continue<A>, T> onCont, F<End<A>, T> onEnd);

        abstract public void foldEffect(Effect1<Continue<A>> onContinue, Effect1<End<A>> onEnd);


    }

    static <A> Next<A> cont(A emit, Supplier<Stream<A>> next) {
        return new Continue<>(emit, next);
    }

    static <A> Next<A> end() {
        return new End<>("Closed", none());
    }

    static <A> Next<A> end(String reason) {
        return new End<>(reason, none());
    }

    static <A> Next<A> end(String reason, Throwable e) {
        return new End<>(reason, some(e));
    }

    class Continue<A> extends Next<A> {
        public final A         value;
        public final Supplier<Stream<A>> next;

        public Continue(A value, Supplier<Stream<A>> next) {
            this.value = value;
            this.next = next;
        }

        @Override
        public <T> T fold(F<Continue<A>, T> onCont, F<End<A>, T> onEnd) {
            return onCont.f(this);
        }

        @Override
        public void foldEffect(Effect1<Continue<A>> onContinue, Effect1<End<A>> onEnd) {
            onContinue.f(this);
        }

    }

    class End<A> extends Next<A> {

        public final String            reason;
        public final Option<Throwable> maybeException;

        public End(String reason, Option<Throwable> maybeException) {
            this.reason = reason;
            this.maybeException = maybeException;
        }

        @Override
        public <T> T fold(F<Continue<A>, T> onCont, F<End<A>, T> onEnd) {
            return onEnd.f(this);
        }

        @Override
        public void foldEffect(Effect1<Continue<A>> onContinue, Effect1<End<A>> onEnd) {
            onEnd.f(this);
        }
    }

    Stop stop = new Stop() {
    };

    interface Stop {
    }

}
