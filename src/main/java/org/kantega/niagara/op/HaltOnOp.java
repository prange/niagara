package org.kantega.niagara.op;

import fj.P;
import fj.P2;
import org.kantega.niagara.Impulse;
import org.kantega.niagara.blocks.Block;

public class HaltOnOp<A> implements Op<A, A> {

    final Impulse impulse;

    public HaltOnOp(Impulse impulse) {
        this.impulse = impulse;
    }

    @Override
    public P2<Scope, Block<A>> build(Scope scope, Block<A> block) {
        return P.p(scope.haltOn(impulse), block);
    }
}
