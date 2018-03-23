package org.kantega.niagara;

import fj.F;
import fj.F2;
import fj.P;
import fj.P2;

/**
 * The interface of a mealy machine.
 * @param <A> The type of the input
 * @param <B> The type of the result
 */
public interface Mealy<A, B> {

    Transition<A, B> apply(A a);

    static <S, A, B> Mealy<A, B> asMealy(S initState, F2<S, A, P2<S, B>> f) {
        return new StateMealy<>(initState, f);
    }

    static <A, B> Transition<A, B> transition(Mealy<A, B> next, B output) {
        return new Transition<>(next, output);
    }

    static <A, B> Transition<A, B> transition(P2<? extends Mealy<A, B>, B> pair) {
        return transition(pair._1(), pair._2());
    }

    default <C> Mealy<P2<C, A>, P2<C, B>> onRight() {
        return pair -> {
            Transition<A, B> t = apply(pair._2());
            return transition(t.nextState.onRight(), P.p(pair._1(), t.output));
        };
    }

    default <C> Mealy<A, C> map(F<B, C> f) {
        return a -> {
            Transition<A, B> next = apply(a);
            return new Transition<>(next.nextState.map(f), f.f(next.output));
        };
    }

    class Transition<A, B> {
        public final B           output;
        public final Mealy<A, B> nextState;

        public Transition(Mealy<A, B> nextState, B output) {
            this.output = output;
            this.nextState = nextState;
        }

        public P2<Mealy<A, B>, B> toTuple() {
            return P.p(nextState, output);
        }
    }

    class StateMealy<S, A, B> implements Mealy<A, B> {

        final S                  state;
        final F2<S, A, P2<S, B>> f;

        public StateMealy(S state, F2<S, A, P2<S, B>> f) {
            this.state = state;
            this.f = f;
        }

        @Override
        public Transition<A, B> apply(A a) {
            P2<S, B> next = f.f(state, a);
            return Mealy.transition(new StateMealy<>(next._1(), f), next._2());
        }
    }

}
