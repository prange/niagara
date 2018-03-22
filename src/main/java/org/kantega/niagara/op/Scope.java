package org.kantega.niagara.op;

import fj.Unit;
import org.kantega.niagara.blocks.Block;
import org.kantega.niagara.blocks.LoopBlock;

import java.util.Optional;

public interface Scope {

    Optional<Scope> parent();


    default Block<Unit> build(Block<Unit> scoped) {
        return parent()
          .map(parent -> parent.build(new LoopBlock(getFlag(), scoped)))
          .orElse(scoped);
    }

    ScopeFlag getFlag();

    default Scope child() {
        return new ChildScope(this);
    }

    static Scope root() {
        return new RootScope();
    }

    class ChildScope implements Scope {

        final Scope parent;
        final ScopeFlag flag;

        public ChildScope(Scope parent) {
            this.parent = parent;
            flag = ScopeFlag.child(parent.getFlag());
        }

        @Override
        public Optional<Scope> parent() {
            return Optional.of(parent);
        }


        @Override
        public ScopeFlag getFlag() {
            return flag;
        }
    }

    class RootScope implements Scope {
        final ScopeFlag flag;

        public RootScope() {
            flag = ScopeFlag.root();
        }

        @Override
        public Optional<Scope> parent() {
            return Optional.empty();
        }

        @Override
        public ScopeFlag getFlag() {
            return flag;
        }


    }
}
