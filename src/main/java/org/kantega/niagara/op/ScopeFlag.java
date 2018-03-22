package org.kantega.niagara.op;

public interface ScopeFlag {

    boolean keepRunning();

    void halt();

    static ScopeFlag child(ScopeFlag parent) {
        return new ChildScopeFlag(parent);
    }

    static ScopeFlag root() {
        return new RootScopeFlag();
    }


    class RootScopeFlag implements ScopeFlag {
        volatile boolean running = true;

        public RootScopeFlag() {
        }

        public boolean keepRunning() {
            return running;
        }

        @Override
        public void halt() {
            running = false;
        }

    }

    class ChildScopeFlag implements ScopeFlag {
        final ScopeFlag parent;
        volatile boolean running = true;

        public ChildScopeFlag(ScopeFlag parent) {
            this.parent = parent;
        }

        @Override
        public boolean keepRunning() {
            return running && parent.keepRunning();
        }

        @Override
        public void halt() {
            running = false;
        }

    }
}
