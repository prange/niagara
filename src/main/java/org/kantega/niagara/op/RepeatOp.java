package org.kantega.niagara.op;

import fj.Unit;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.RepeatBlock;

public class RepeatOp<A> implements Op<Unit, A> {

    final Op<Unit, A> repeated;

    public RepeatOp(Op<Unit, A> repeated) {
        this.repeated = repeated;
    }


    @Override
    public Block<Unit> build(Scope scope, Block<A> block) {
        return scope.child(child -> new RepeatBlock(scope, child, repeated.build(child, block)));
    }
}
