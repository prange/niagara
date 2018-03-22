package org.kantega.niagara.blocks;

import org.kantega.niagara.op.ScopeFlag;

public interface OutputBlock {

    void run(ScopeFlag signal);

}
