package org.kantega.niagara.op;

import fj.Unit;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.PollBlock;

import java.util.Queue;

public class PollOp<A> implements StageOp<Unit, A> {

    final Queue<A> queue;

    public PollOp(Queue<A> queue) {
        this.queue = queue;
    }

    @Override
    public Block<Unit> build(Scope scope, Block<A> block) {
        return new PollBlock<>(queue, block);
    }
}
