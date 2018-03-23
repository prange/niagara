package org.kantega.niagara;

import java.util.concurrent.CompletableFuture;

public class Impulse {

    final CompletableFuture<Boolean> wrapped;

    public Impulse(CompletableFuture<Boolean> wrapped) {
        this.wrapped = wrapped;
    }

    public void onImpulse(Runnable r){
        wrapped.thenRun(r);
    }


}
