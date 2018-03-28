package org.kantega.niagara.op;

import java.util.function.Function;

public interface Scope {

    void reset();

    boolean keepRunning();

    void halt();


    default Scope child() {
        return new ChildScope(this);
    }

    default <A> A child(Function<Scope,A> childHandler){
        return childHandler.apply(child());
    }

    static Scope root() {
        return new RootScope();
    }

    class ChildScope implements Scope {

        final Scope parent;
        volatile boolean running = true;

        public ChildScope(Scope parent) {
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

        @Override
        public void reset() {
            running = true;
        }


    }

    class RootScope implements Scope {
        volatile boolean running = true;

        public RootScope() {

        }

        @Override
        public boolean keepRunning() {
            return running;
        }

        @Override
        public void halt() {
            running = false;
        }

        @Override
        public void reset() {
            running = true;
        }

    }

}
