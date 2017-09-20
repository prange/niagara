package org.kantega.niagara.example;

import fj.Unit;
import org.kantega.niagara.Source;
import org.kantega.niagara.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.kantega.niagara.example.Utils.*;

public class Example6_join {



    static final ExecutorService pool =
      Executors.newFixedThreadPool(2);

    public static void main(String[] args) {

        AtomicLong counter = new AtomicLong();

        Task<String> print =
          read(counter)
            .flatMap(sum -> println("The sum is " + sum));

        Source<Unit> dbSource =
          new AsyncFakeDb(pool).join(new AsyncFakeDb(pool))
            .foldLeft(0L, (count, str) -> count + 1)
            .apply(sum -> set(counter, sum))
            .onClose(print);

        dbSource.toTask().execute();
    }
}
