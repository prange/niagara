package org.kantega.niagara.task;

import fj.P;
import fj.P2;
import fj.data.Either;
import org.kantega.niagara.Try;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class Join<A, B, C, D> {

    // (Try a, Fiber b) V (Fiber a, Try b) -> Action c
    private final Function<Either<P2<Try<A>, Fiber<B>>, P2<Fiber<A>, Try<B>>>, Action<C>> handler;
    private final Fiber<A> aFiber;
    private final Fiber<B> bFiber;
    private final RTS rts;
    private final AtomicReference<JoinStatus> ref =
      new AtomicReference<>(new JoinStatus(false, false));
    private final Optional<Function<Try<C>, Action<D>>> mayebCont;

    public Join(Function<Either<P2<Try<A>, Fiber<B>>, P2<Fiber<A>, Try<B>>>, Action<C>> handler, Fiber<A> aFiber, Fiber<B> bFiber, RTS rts, Optional<Function<Try<C>, Action<D>>> mayebCont) {
        this.handler = handler;
        this.aFiber = aFiber;
        this.bFiber = bFiber;
        this.rts = rts;
        this.mayebCont = mayebCont;
    }


    public void left(Try<A> aTry) {
        var status = ref.updateAndGet(JoinStatus::setLeft);
        if (status.isComplete())
            rts.runAction(handler.apply(Either.left(P.p(aTry, bFiber))));
        //TODO, legg på continuation her.
    }

    public void right(Try<B> bTry) {
        var status = ref.updateAndGet(JoinStatus::setRight);
        if (status.isComplete())
            rts.runAction(handler.apply(Either.right(P.p(aFiber, bTry))));
        //TODO, legg på continuation her.
    }

    static class JoinStatus {
        final boolean aSet;
        final boolean bSet;

        JoinStatus(boolean aSet, boolean bSet) {
            this.aSet = aSet;
            this.bSet = bSet;
        }


        public JoinStatus setLeft() {
            return new JoinStatus(true, bSet);
        }

        public JoinStatus setRight() {
            return new JoinStatus(aSet, true);
        }

        boolean isComplete() {
            //xor
            return aSet && !bSet || !aSet && bSet;
        }
    }
}
