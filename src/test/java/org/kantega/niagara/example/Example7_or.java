package org.kantega.niagara.example;

import fj.Unit;
import fj.data.Either;
import org.kantega.niagara.Source;
import org.kantega.niagara.Sources;
import org.kantega.niagara.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.kantega.niagara.example.Utils.*;

public class Example7_or {



    static final ExecutorService pool =
      Executors.newFixedThreadPool(2);

    public static void main(String[] args) {

        AtomicLong counter = new AtomicLong();

        Task<String> print =
          read(counter)
            .flatMap(sum -> println("The sum is " + sum));

        Source<String> strings1 =
          Sources.values("one", "two", "three");

        Source<Unit> dbSource =
          new AsyncFakeDb(pool).or(strings1)
            .foldLeft(0L, (count, str) -> count + 1)
            .apply(sum -> set(counter, sum))
            .onClose(print);

        dbSource.toTask().execute();
    }
}
