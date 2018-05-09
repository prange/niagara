package org.kantega.niagara.task;

import fj.Unit;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class TaskRuntime {

    final ExecutorService executorService =
      Executors.newFixedThreadPool(2);

    final ScheduledExecutorService scheduledExecutorService =
      Executors.newSingleThreadScheduledExecutor();

    final Function<Throwable, Task<Unit>> defaultHandler =
      t -> Task.run(t::printStackTrace);


    public <A> void eval(Task<A> task) {
        enqueue(() ->
          task.eval(
            new TaskContext.RootContext(this),
            aTry -> aTry.doEffect(t -> eval(defaultHandler.apply(t)), a -> {})));
    }

    public <A> void enqueue(Runnable r) {
        executorService.submit(r);
    }

    public void schedule(Runnable r, Duration d) {
        scheduledExecutorService.schedule(() -> executorService.submit(r), d.toMillis(), TimeUnit.MILLISECONDS);
    }


}
