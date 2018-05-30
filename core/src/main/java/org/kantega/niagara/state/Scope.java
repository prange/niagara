package org.kantega.niagara.state;

import org.kantega.niagara.source.Done;

import java.util.function.Consumer;

public class Scope<O> {

    public final Consumer<O> consumer;
    public final Done<O> done;
    private boolean running;

    private Scope(Consumer<O> consumer, Done<O> done) {
        this.consumer = consumer;
        this.running = true;
        this.done = done;
    }


    public static <A> Scope<A> scope(Consumer<A> consumer, Done<A> done) {
        return new Scope<>(consumer, done);
    }

    public Scope<O> onDone(Runnable action) {
        return scope(consumer, d -> {
            action.run();
            done.done(d);
        });
    }

    public boolean isRunning() {
        return running;
    }


    public void halt() {
        running = false;
    }

}
