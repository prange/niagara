package org.kantega.niagara.exchange;

import fj.Unit;
import org.kantega.niagara.Eventually;
import org.kantega.niagara.Source;
import org.kantega.niagara.SourceListener;
import org.kantega.niagara.Task;

import java.util.concurrent.CopyOnWriteArrayList;

public class SyncTopic<A> {

    CopyOnWriteArrayList<SourceListener<A>> listeners = new CopyOnWriteArrayList<>();

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
        return Task.runnableTask(() -> listeners.forEach(listener -> listener.handle(value)));
    }

}
