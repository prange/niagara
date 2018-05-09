package org.kantega.niagara.task;

import fj.P;
import fj.P2;

import java.time.Duration;

public interface TaskContext {
    void interrupt();

    boolean isInterrupted();

    void enqueue(Runnable r);

    void schedule(Runnable task, Duration d);

    P2<TaskContext, TaskContext> branch();

    class RootContext implements TaskContext {


        private boolean interrupted = false;
        private final TaskRuntime rt;
        private P2<BranchedContext, BranchedContext> children;

        public RootContext(TaskRuntime rt) {
            this.rt = rt;
        }

        public void interrupt() {
            interrupted = true;
            if (children != null) {
                children._1().interrupted = true;
                children._2().interrupted = true;
            }
        }

        @Override
        public boolean isInterrupted() {
            return interrupted;
        }

        public void enqueue(Runnable r) {
            rt.enqueue(r);
        }

        public void schedule(Runnable task, Duration d) {
            rt.schedule(task, d);
        }

        public P2<TaskContext, TaskContext> branch() {
            children = P.p(new BranchedContext(rt), new BranchedContext(rt));
            return children.<TaskContext>map1(i -> i).map2(i -> i);
        }
    }

    class BranchedContext implements TaskContext {


        private final TaskRuntime rt;
        private boolean interrupted = false;

        public BranchedContext(TaskRuntime rt) {
            this.rt = rt;
        }


        @Override
        public void interrupt() {
            interrupted = true;
        }

        @Override
        public boolean isInterrupted() {
            return interrupted ;
        }

        @Override
        public void enqueue(Runnable r) {
            rt.enqueue(r);
        }

        @Override
        public void schedule(Runnable task, Duration d) {
            rt.schedule(task, d);
        }

        @Override
        public P2<TaskContext, TaskContext> branch() {
            return null;
        }
    }
}
