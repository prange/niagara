package org.kantega.niagara.op;

import fj.Unit;
import org.kantega.niagara.Impulse;
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

    default Scope haltOn(Impulse impulse) {
        return new HaltOnScope(this, impulse);
    }

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

    class HaltOnScope implements Scope {

        final Scope wrapped;
        final Impulse impulse;

        public HaltOnScope(Scope wrapped, Impulse impulse) {
            this.wrapped = wrapped;
            this.impulse = impulse;
        }

        @Override
        public Optional<Scope> parent() {
            return wrapped.parent();
        }

        @Override
        public ScopeFlag getFlag() {
            ScopeFlag flag = wrapped.getFlag();
            impulse.onImpulse(flag::halt);
            return flag;
        }
    }
}
