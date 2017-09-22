package org.kantega.niagara.example;

import org.kantega.niagara.Source;
import org.kantega.niagara.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.kantega.niagara.example.Utils.*;

public class Example5_append {



    static final ExecutorService pool =
      Executors.newFixedThreadPool(2);

    public static void main(String[] args) {

        AtomicLong counter = new AtomicLong();

        Task<String> print =
          read(counter)
            .flatMap(sum -> println("The sum is " + sum));

        Source<?> dbSource =
          new AsyncFakeDb(pool)
            .onClose(println("Async closed").toUnit())
            .append(SyncFakeDb::new) //Append
            .foldLeft(0L, (count, str) -> count + 1)
            .apply(sum -> set(counter, sum))
            .onClose(print );

        dbSource.toTask().execute();
    }
}
