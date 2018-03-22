package org.kantega.niagara.blocks;

import fj.Unit;
import org.kantega.niagara.op.ScopeFlag;

public class LoopBlock implements Block<Unit> {

    final ScopeFlag flag;
    final Block<Unit> inner;

    public LoopBlock(ScopeFlag flag, Block<Unit> inner) {
        this.flag = flag;
        this.inner = inner;
    }

    @Override
    public void run(Unit input) {
        while (flag.keepRunning())
            inner.run(Unit.unit());
    }
}
