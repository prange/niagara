package org.kantega.niagara;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a untyped signal
 */
public class Interrupt {

    final CompletableFuture<Boolean> wrapped;

    public Interrupt(CompletableFuture<Boolean> wrapped) {
        this.wrapped = wrapped;
    }

    public void onImpulse(Runnable r){
        wrapped.thenRun(r);
    }

    public boolean isInterrupted(){
        return wrapped.getNow(false);
    }


}
