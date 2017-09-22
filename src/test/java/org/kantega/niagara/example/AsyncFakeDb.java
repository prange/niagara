package org.kantega.niagara.example;

import fj.data.Stream;
import org.kantega.niagara.Eventually;
import org.kantega.niagara.Source;
import org.kantega.niagara.SourceListener;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class AsyncFakeDb implements Source<String> {

    private final Random r = new Random();
    private final ExecutorService pool;

    public AsyncFakeDb(ExecutorService pool) {
        this.pool = pool;
    }

    @Override
    public Eventually<Running> open(SourceListener<String> listener) {
        return Eventually.async(pool, startingCallback -> {
            System.out.println("Faking open async database");

            Eventually<Result> eventuallyClosing =
              Eventually.async(pool, callback -> {
                  AtomicReference<Result> lastResult = new AtomicReference<>(Result.ack);

                  Stream.range(0, 1000000).foreachDoEffect(n -> {
                      String randString = n + " " + r.nextLong();

                      lastResult.set(listener.handle(randString).await(Duration.ofSeconds(2)).fold(t -> Result.closed, r -> r));

                  });

                  System.out.println("Faking close async database");
                  callback.complete(lastResult.get());
              });

            startingCallback.complete(new Running(eventuallyClosing));
        });


    }
}
