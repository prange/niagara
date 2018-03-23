package org.kantega.niagara.op;

import fj.P;
import fj.P2;
import fj.Unit;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.PullRepeatBlock;
import org.kantega.niagara.concurrent.WaitStrategy;

import java.util.Queue;
import java.util.function.Supplier;

public class PullRepeatOp<A> implements Op<Unit, A> {

    final Queue<A> supplier;
    final Supplier<WaitStrategy> waitStrategy;
    final Supplier<WaitStrategy> idleStrategy;

    public PullRepeatOp(Queue<A> supplier, Supplier<WaitStrategy> waitStrategy, Supplier<WaitStrategy> idleStrategy) {
        this.supplier = supplier;
        this.waitStrategy = waitStrategy;
        this.idleStrategy = idleStrategy;
    }


    @Override
    public P2<Scope, Block<Unit>> build(Scope scope, Block<A> block) {
        return P.p(scope, new PullRepeatBlock<>(scope.getFlag(), supplier, waitStrategy.get(), idleStrategy.get(), block));
    }
}
