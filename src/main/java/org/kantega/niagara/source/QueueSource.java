package org.kantega.niagara.source;

import org.kantega.niagara.Emitter;
import org.kantega.niagara.Source;
import org.kantega.niagara.sink.Sink;

import java.util.Queue;

public final class QueueSource<O> implements Source<O> {

    final Queue<O> queue;

    public QueueSource(Queue<O> queue) {
        this.queue = queue;
    }

    @Override
    public Emitter build(Sink<O> sink) {
        return () -> {
            O v = queue.poll();
            if (v != null) {
                sink.consumer.accept(v);
                return true;
            } else
                return false;
        };
    }
}
