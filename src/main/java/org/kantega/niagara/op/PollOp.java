package org.kantega.niagara.op;

import fj.P;
import fj.P2;
import fj.Unit;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.PollBlock;

import java.util.Queue;

public class PollOp<A> implements Op<Unit, A> {

    final Queue<A> queue;

    public PollOp(Queue<A> queue) {
        this.queue = queue;
    }

    @Override
    public P2<Scope, Block<Unit>> build(Scope scope, Block<A> block) {
        return P.p(scope, new PollBlock<>(queue, block));
    }
}