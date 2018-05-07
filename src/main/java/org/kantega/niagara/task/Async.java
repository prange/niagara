package org.kantega.niagara.task;

import java.util.function.Consumer;

public interface Async<A> {


    class Uninterruptable<A> implements Async<A> {

    }

    class Interruptable<A> implements Async<A> {
        public final Consumer<Throwable> interruptor;

        public Interruptable(Consumer<Throwable> interruptor) {
            this.interruptor = interruptor;
        }
    }
}
