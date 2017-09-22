package org.kantega.niagara.exchange;

import fj.Unit;
import org.kantega.niagara.Eventually;
import org.kantega.niagara.Source;
import org.kantega.niagara.SourceListener;
import org.kantega.niagara.Task;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.kantega.niagara.Task.*;

public class AsyncDroppingInputQueue<A> {

    private final long       bounds;
    private final AtomicLong size;
    private final Executor   executor;
    private final int                                     max       = 100;
    private final AtomicBoolean                           working   = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<A>                mbox      = new ConcurrentLinkedQueue<>();
    private final CopyOnWriteArrayList<SourceListener<A>> listeners = new CopyOnWriteArrayList<>();

    public AsyncDroppingInputQueue(int bounds, Executor executor) {
        this.size = new AtomicLong();
        this.executor = executor;
        this.bounds = bounds;
    }


    public Task<Unit> offer(A a) {
        return runnableTask(() -> {
            if (size.incrementAndGet() > bounds) {
                mbox.poll();
                mbox.offer(a);
                size.decrementAndGet();
            } else {
                mbox.offer(a);
            }
            work();
        });
    }

    private void work() {
        if (!mbox.isEmpty() && !listeners.isEmpty() && working.compareAndSet(false, true)) {
            executor.execute(() -> {
                long count = 0;
                while (!Thread.currentThread().isInterrupted() && !mbox.isEmpty() && !listeners.isEmpty() && count++ < max) {
                    A a = mbox.poll();
                    size.decrementAndGet();
                    listeners.forEach(listener -> listener.handle(a));
                }
                working.set(false);
                work();
            });
        }

    }

    public Source<A> subscribe() {
        return handler -> call(() -> {
            listeners.add(handler);
            work();
            return
              new Source.Running(Eventually.never()).onStop(runnableTask(() -> listeners.remove(handler)));
        }).using(executor).execute();
    }
}
