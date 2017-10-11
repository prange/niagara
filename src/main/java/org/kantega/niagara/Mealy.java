package org.kantega.niagara;

import fj.P;
import fj.P2;

public interface Mealy<A, B> {

    <M extends Mealy<A,B>> Transition<A,B,M> apply(A a);

    default <M extends Mealy<A,B>> Transition<A,B,M> transition(B output,M next){
        return new Transition<>(output,next);
    }

    class Transition<A,B,M extends Mealy<A,B>>{
        public final B output;
        public final M nextState;

        public Transition(B output, M nextState) {
            this.output = output;
            this.nextState = nextState;
        }

        public P2<M,B> toTuple(){
            return P.p(nextState,output);
        }
    }

}
