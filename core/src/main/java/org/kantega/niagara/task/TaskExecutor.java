package org.kantega.niagara.task;

import fj.Unit;
import org.kantega.niagara.Try;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class TaskExecutor {

    final ForkJoinPool executorService =
      ForkJoinPool.commonPool();

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
        executorService.submit(() -> {
            try {
                t.perform(tc, continuation);
            } catch (Throwable e) {
                continuation.accept(Try.fail(e));
            }
        });
    }

    public <A> CompletableFuture<Unit> enqueueStage(TaskContext tc, Task<A> t, Consumer<Try<A>> continuation) {
        var cf = new CompletableFuture<Unit>();
        executorService.submit(() -> {
            try {
                t.perform(tc, continuation);
                cf.complete(Unit.unit());
            } catch (Throwable e) {
                continuation.accept(Try.fail(e));
            }
        });
        return cf;
    }

    public <A> void schedule(TaskContext tc, Task<A> t, Consumer<Try<A>> continuation, Instant at) {
        Duration d = Duration.between(Instant.now(), at);
        if (d.isNegative())
            enqueue(tc, t, continuation);
        else
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
