package org.kantega.niagara.exchange;

import fj.Unit;
import fj.data.List;
import org.kantega.niagara.*;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

import static org.kantega.niagara.Task.*;

public class AsyncDroppingInputQueue<A> {

    private final long       bounds;
    private final AtomicLong size;
    private final Executor   executor;
    private final int                      max     = 100;
    private final AtomicBoolean            working = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<A> mbox    = new ConcurrentLinkedQueue<>();
    private final Topic<A>                 topic   = new Topic<>();

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
        if (!mbox.isEmpty() && !topic.isEmpty() && working.compareAndSet(false, true)) {
            executor.execute(() -> {
                long count = 0;
                while (!Thread.currentThread().isInterrupted() && !mbox.isEmpty() && !topic.isEmpty() && count++ < max) {
                    A a = mbox.poll();
                    size.decrementAndGet();
                    topic.publish(a).execute().await(Sources.defaultTimeout).doEffect(Throwable::printStackTrace, i -> {});
                }
                working.set(false);
                work();
            });
        }
    }

    public Source<A> subscribe() {
        return (closer, handler) -> {
            Eventually<Source.Closed> closed =
              topic.subscribe().open(closer, handler);
            work();
            return closed;
        };
    }
}
