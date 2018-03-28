package org.kantega.niagara.blocks;

import fj.Unit;
import org.kantega.niagara.op.Scope;

public class RepeatBlock implements Block<Unit> {

    final Scope scope;
    final Scope innerScope;
    final Block<Unit> inner;

    public RepeatBlock(Scope scope, Scope innerScope, Block<Unit> inner) {
        this.scope = scope;
        this.innerScope = innerScope;
        this.inner = inner;
    }

    @Override
    public void run(Unit input) {

        while (scope.keepRunning()) {
            inner.run(Unit.unit());
            innerScope.reset();
        }
    }
}
