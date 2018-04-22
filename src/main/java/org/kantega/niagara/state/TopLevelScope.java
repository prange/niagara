package org.kantega.niagara.state;

import org.kantega.niagara.Try;
import org.kantega.niagara.sink.Sink;
import org.kantega.niagara.thread.WaitStrategy;

import java.util.Optional;

public class TopLevelScope<O> implements Scope<O> {

    public final Sink<O> sink;
    public final WaitStrategy waitStrategy;
    private Step<O> step;
    private Instruction<O, Optional<Instruction>> scope;
    private boolean running = false;

    public TopLevelScope(Instruction<O, Optional<Instruction>> scope, Sink<O> sink, WaitStrategy waitStrategy) {
        this.scope = scope;
        this.sink = sink;
        this.waitStrategy = waitStrategy;
    }


    public void loop() {
        step = scope.build(this).stepper(this::halt);
        while (running) {
            step.step();
        }
    }

    @Override
    public Sink<O> sink() {
        return sink;
    }

    @Override
    public WaitStrategy waitStrategy() {
        return waitStrategy;
    }

    public void setNext(Instruction<O, ?> next) {
        waitStrategy.reset();
        step = next.build(this).stepper(this::halt);

    }


    private void halt(Try<?> t) {
         this.running = false;
    }

    private Step<O> applyWait() {
        return waitStrategy::idle;
    }

    private Step<O> resetWait() {
        return waitStrategy::reset;
    }

}
