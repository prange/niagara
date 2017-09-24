package org.kantega.niagara.example;

import fj.F;
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

import static fj.F2Functions.tuple;
import static org.kantega.niagara.Task.runnableTask;
import static org.kantega.niagara.example.Utils.println;
import static org.kantega.niagara.example.Utils.read;

public class Example9_queue {

    static final ExecutorService pool =
      Executors.newFixedThreadPool(1);

    public static void main(String[] args) throws InterruptedException {


        AtomicLong counter =
          new AtomicLong();

        AsyncDroppingInputQueue<String> queue =
          new AsyncDroppingInputQueue<>(10, pool);

        long numberOfElements = 100;

        F<String, Task<Unit>> increment =
          u -> Task.runnableTask(counter::incrementAndGet);

        Task<Unit> print =
          read(counter)
            .flatMap(count ->
              println("The sum is " + count)).toUnit();

        Source<?> qSource =
          queue
            .subscribe()
            .apply(increment)
            .zipWithIndex()
            .asLongAs(tuple((index, value) -> index < numberOfElements))
            .onClose(print);

        //Open the source, so we get the values in the queue
        qSource.toTask().execute();

        Stream.range(0, numberOfElements).foreachDoEffect(n -> {
            queue.offer("Message " + n).execute().await(Duration.ofSeconds(1));
        });



    }
}
