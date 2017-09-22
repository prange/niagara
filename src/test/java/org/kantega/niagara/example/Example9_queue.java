package org.kantega.niagara.example;

import fj.Unit;
import fj.data.Stream;
import org.kantega.niagara.Source;
import org.kantega.niagara.Task;
import org.kantega.niagara.exchange.AsyncDroppingInputQueue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.kantega.niagara.Task.*;
import static org.kantega.niagara.example.Utils.println;
import static org.kantega.niagara.example.Utils.read;

public class Example9_queue {

    static final ExecutorService pool =
      Executors.newFixedThreadPool(1);

    public static void main(String[] args) throws InterruptedException {

        CountDownLatch keepFromExiting = new CountDownLatch(1);

        AtomicLong counter =
          new AtomicLong();

        AsyncDroppingInputQueue<String> queue =
          new AsyncDroppingInputQueue<>(1000, pool);

        Task<Unit> print =
          read(counter)
            .flatMap(sum -> println("The sum is " + sum))
            .flatMap(string -> Task.tryRunnableTask(() -> Thread.sleep(100)));

        Source<?> qSource =
          queue
            .subscribe()
            .apply(Utils::println)
            .onClose(print)
            .onClose(runnableTask(keepFromExiting::countDown));



        Stream.range(0, 1000000).foreachDoEffect(n -> {
            queue.offer("Message " + n).execute();
        });

        qSource.toTask().execute().onComplete(runningAttempt -> runningAttempt.toOption().foreachDoEffect(Source.Running::stop));

        //keepFromExiting.await();
    }
}
