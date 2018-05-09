package org.kantega.niagara.task;

import fj.P;
import fj.P2;
import fj.data.Either;
import org.kantega.niagara.Try;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

public class Gate<A, B, C> {

    private final Consumer<Try<C>> cont;

    // (Try a, Fiber b) \/ (Fiber a, Try b) -> Action c
    private final Function<Either<P2<Try<A>, Strand<B>>, P2<Strand<A>, Try<B>>>, Task<C>> handler;
    private final Strand<A> aStrand;
    private final Strand<B> bStrand;
    private final TaskContext rt;
    private final AtomicBoolean wasRun = new AtomicBoolean(false);

    public Gate(Function<Either<P2<Try<A>, Strand<B>>, P2<Strand<A>, Try<B>>>, Task<C>> handler, Strand<A> aStrand, Strand<B> bStrand, TaskContext rt, Consumer<Try<C>> cont) {
        this.handler = handler;
        this.aStrand = aStrand;
        this.bStrand = bStrand;
        this.rt = rt;
        this.cont = cont;
    }


    public void left(Try<A> aTry) {
        if (!wasRun.getAndSet(true)) {
            Task<C> cTask = handler.apply(Either.left(P.p(aTry, bStrand)));
            cTask.eval(rt, cont);
        }
    }

    public void right(Try<B> bTry) {
        if (!wasRun.getAndSet(true)) {
            Task<C> cTask = handler.apply(Either.right(P.p(aStrand, bTry)));
            cTask.eval(rt, cont);
        }
    }
}
