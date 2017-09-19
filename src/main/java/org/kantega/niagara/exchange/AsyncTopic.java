package org.kantega.niagara.exchange;

import fj.Unit;
import fj.data.List;
import org.kantega.niagara.Eventually;
import org.kantega.niagara.Source;
import org.kantega.niagara.SourceListener;
import org.kantega.niagara.Task;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

public class AsyncTopic<A> {

    private final Executor executor;
    private final CopyOnWriteArrayList<SourceListener<A>> listeners =
      new CopyOnWriteArrayList<>();

    public AsyncTopic(Executor executor) {
        this.executor = executor;
    }

    /**
     * @return
     */
    public Source<A> subscribe() {
        return handler -> {
            listeners.add(handler);
            return
              Eventually.value(
                new Source.Running(Eventually.never()).onStop(Task.runnableTask(() -> {
                    listeners.remove(handler);
                })));
        };
    }

    public Task<Unit> publish(A value) {
        return Task.async(callback -> Eventually.sequenceList(List.iterableList(listeners).map(listeners->listeners.handle(value))).onComplete(att->callback.f(att.map(l->Unit.unit()))));
    }
}
