package org.kantega.niagara.task;

import fj.Unit;
import org.kantega.niagara.Try;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class TaskRuntime {

    final ExecutorService executorService =
      Executors.newFixedThreadPool(2);

    final ScheduledExecutorService scheduledExecutorService =
      Executors.newSingleThreadScheduledExecutor();

    final Function<Throwable, Task<Unit>> defaultHandler =
      t -> Task.run(t::printStackTrace);


    public <A> void eval(Task<A> task) {
        enqueue(
          new TaskContext(this),
          task,
          aTry -> aTry.doEffect(t -> eval(defaultHandler.apply(t)), a -> {}));
    }

    public <A> void enqueue(TaskContext tc, Task<A> t, Consumer<Try<A>> continuation) {
        executorService.submit(() -> t.perform(tc, continuation));
    }

    public <A> void schedule(TaskContext tc, Task<A> t, Consumer<Try<A>> continuation, Duration d) {
        scheduledExecutorService.schedule(() -> enqueue(tc, t, continuation), d.toMillis(), TimeUnit.MILLISECONDS);
    }


    public void shutdown() {
        try {
            scheduledExecutorService.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            executorService.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
