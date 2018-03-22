package org.kantega.niagara.blocks;

import org.kantega.niagara.op.ScopeFlag;

public interface PairBlock<A,B> {

    void run(ScopeFlag signal, A a , B b);

}
