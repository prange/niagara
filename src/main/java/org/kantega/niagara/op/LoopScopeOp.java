package org.kantega.niagara.op;

import fj.P;
import fj.P2;
import fj.Unit;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.LoopBlock;

public class LoopScopeOp implements Op<Unit, Unit> {

    final Scope scope;

    public LoopScopeOp(Scope scope) {
        this.scope = scope;
    }


    @Override
    public <C> Op<Unit, C> fuse(Op<Unit, C> next) {
        return new ComposedOp<>(this, next);
    }

    @Override
    public P2<Scope, Block<Unit>> build(Scope scope, Block<Unit> block) {
        Scope child = scope.child();
        return P.p(child, new LoopBlock(child.getFlag(), block));
    }
}
