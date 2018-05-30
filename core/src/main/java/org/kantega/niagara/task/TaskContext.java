package org.kantega.niagara.task;

import fj.P;
import fj.P2;
import fj.Unit;
import org.kantega.niagara.Try;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TaskContext {

    private boolean interrupted = false;
    private final TaskExecutor rt;
    private P2<TaskContext, TaskContext> children;

    public TaskContext(TaskExecutor rt) {
        this.rt = rt;
    }

    public void interrupt() {
        interrupted = true;
        if (children != null) {
            children._1().interrupted = true;
            children._2().interrupted = true;
        }
    }

    public boolean isInterrupted(){
        return interrupted;
    }

    public <A> void enqueue(Task<A> t, Consumer<Try<A>> continuation) {
        rt.enqueue(this, t, aTry -> {
            if (!interrupted || aTry.isThrowable()) //Errors are always passed
                continuation.accept(aTry);
            else
                continuation.accept(Try.fail(new InterruptedException("The task "+t.toString()+" was interrupted")));
        });
    }

    public <A> CompletableFuture<Unit> enqueueStage(Task<A> t, Consumer<Try<A>> continuation) {
        return rt.enqueueStage(this,t,continuation);
    }
    public <A> void schedule(Task<A> t, Consumer<Try<A>> continuation, Instant i) {
        rt.schedule(this, t, continuation, i);
    }

    public P2<TaskContext, TaskContext> branch() {
        children = P.p(new TaskContext(rt), new TaskContext(rt));
        return children.<TaskContext>map1(i -> i).map2(i -> i);
    }
}



