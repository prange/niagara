package org.kantega.niagara.task;

import fj.P;
import fj.P2;
import org.kantega.niagara.Try;

import java.time.Duration;
import java.util.function.Consumer;

public class TaskContext {

    private boolean interrupted = false;
    private final TaskRuntime rt;
    private P2<TaskContext, TaskContext> children;

    public TaskContext(TaskRuntime rt) {
        this.rt = rt;
    }

    public void interrupt() {
        interrupted = true;
        if (children != null) {
            children._1().interrupted = true;
            children._2().interrupted = true;
        }
    }


    public boolean isInterrupted() {
        return interrupted;
    }

    public <A> void enqueue(Task<A> t, Consumer<Try<A>> continuation) {
        rt.enqueue(this, t, continuation);
    }

    public <A> void schedule(Task<A> t, Consumer<Try<A>> continuation, Duration d) {
        rt.schedule(this, t, continuation, d);
    }

    public P2<TaskContext, TaskContext> branch() {
        children = P.p(new TaskContext(rt), new TaskContext(rt));
        return children.<TaskContext>map1(i -> i).map2(i -> i);
    }
}



