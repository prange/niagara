package org.kantega.niagara;

import fj.P2;
import fj.Unit;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.op.*;

import java.util.function.Function;
import java.util.function.Predicate;

public class Plan<A> {


    public final Op<Unit, A> ops;

    public Plan(Op<Unit, A> ops) {
        this.ops = ops;
    }

    public Block<Unit> build(Scope scope, Block<A> terminator) {
        P2<Scope, Block<Unit>> block = ops.build(scope, terminator);
        return block._1().build(block._2());
    }

    public static <B> Plan<B> plan(Op<Unit, B> ops) {
        return new Plan<>(ops);
    }

    public <B> Plan<B> append(Op<A, B> op) {
        return plan(ops.fuse(op));
    }


    public <B> Plan<B> map(Function<A, B> f) {
        return append(new MapOp<>(f));
    }


    public <B> Plan<B> flatMap(Function<A, Iterable<B>> f) {
        return append(new FlatMapOp<>(f));
    }

    public <B> Plan<B> bind(Function<A, Plan<B>> f) {
        return append(new BindOp<>(f));
    }

    public <B> Plan<B> eval(Function<A, Eval<B>> f) {
        return append(new EvalOp<>(f));
    }

    public Plan<A> takeWhile(Predicate<A> pred) {
        return
          append(new TakeWhileOp<>(pred));
    }

    public Plan<A> repeat() {
        return append(new RepeatOp<>(this));
    }

    public Plan<A> haltOn(Impulse impulse) {
        return append(new HaltOnOp<>(impulse));
    }

    public void run() {
        build(Scope.root(), (a) -> {
        }).run(Unit.unit());
    }

}
