package org.kantega.niagara.task;

import fj.Unit;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Function;

public class RTS {

    final ExecutorService executorService =
      Executors.newFixedThreadPool(2);

    final ScheduledExecutorService scheduledExecutorService =
      Executors.newSingleThreadScheduledExecutor();

    final Function<Throwable, Action<Unit>> defaultHandler =
      t -> Action.run(t::printStackTrace);

    public <A> void runAction(Action<A> action) {
        submit(new ActionRunner<>(RTS.this, defaultHandler).setInitAction(action));
    }

    public <A> Async<A> submit(ActionRunner<A> r) {
        executorService.execute(r::run);
        return new Async.Uninterruptable<>();
    }

    public <A> Async<A> schedule(ActionRunner<A> r, Duration timeout) {
        if (timeout.isZero())
            return submit(r);

        ScheduledFuture<?> fut =
          scheduledExecutorService.schedule(r::run, timeout.toNanos(), TimeUnit.NANOSECONDS);

        return new Async.Interruptable<>(ex -> fut.cancel(true));
    }

    public void halt() {
        scheduledExecutorService.shutdownNow();
        executorService.shutdown();
    }

}
