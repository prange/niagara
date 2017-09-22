package org.kantega.niagara.exchange;

import fj.Unit;
import org.kantega.niagara.Eventually;
import org.kantega.niagara.Source;
import org.kantega.niagara.SourceListener;
import org.kantega.niagara.Task;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.kantega.niagara.Task.call;
import static org.kantega.niagara.Task.runnableTask;

public class AsyncBlockingInputQueue<A> {

    private final Semaphore bounds;
    private final Executor  executor;
    private final int                                     max       = 100;
    private final AtomicBoolean                           working   = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<A>                mbox      = new ConcurrentLinkedQueue<>();
    private final CopyOnWriteArrayList<SourceListener<A>> listeners = new CopyOnWriteArrayList<>();

    public AsyncBlockingInputQueue(int bounds, Executor executor) {
        this.executor = executor;
        this.bounds = new Semaphore(bounds);
    }


    public Task<Unit> offer(A a) {
        return runnableTask(() -> {
            try {
                bounds.acquire();
                mbox.offer(a);
                work();
            } catch (InterruptedException e) {
            }
        });
    }

    private void work() {
        if (!mbox.isEmpty() && !listeners.isEmpty() && working.compareAndSet(false, true)) {
            executor.execute(() -> {
                long count = 0;
                while (!Thread.currentThread().isInterrupted() && !mbox.isEmpty() && !listeners.isEmpty() && count++ < max) {
                    A a = mbox.poll();
                    if (a != null) {
                        bounds.release();
                        listeners.forEach(listener -> listener.handle(a));
                    }
                }
                working.set(false);
                work();
            });
        }

    }

    public Source<A> subscribe() {
        return handler -> call(() -> {
            listeners.add(handler);
            return
              new Source.Running(Eventually.never()).onStop(runnableTask(() -> listeners.remove(handler)));
        }).using(executor).execute();
    }
}
