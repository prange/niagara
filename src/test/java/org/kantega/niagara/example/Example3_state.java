package org.kantega.niagara.example;

import fj.Unit;
import org.kantega.niagara.Source;
import org.kantega.niagara.Task;

import java.util.concurrent.atomic.AtomicLong;

public class Example3_state {

    static Task<Unit> set(AtomicLong atomicLong, long value) {
        return Task.runnableTask(() -> atomicLong.set(value));
    }

    static Task<Long> read(AtomicLong atomicLong) {
        return Task.call(atomicLong::get);
    }

    static Task<String> println(String line) {
        return Task.call(() -> {
            System.out.println(line);
            return line;
        });
    }

    public static void main(String[] args) {

        AtomicLong counter = new AtomicLong();

        //Constructing a source that reads from a db,
        //folds over its values and sets a counter
        Source<?> dbSource =
          new SyncFakeDb()
            .foldLeft(0L, (count, str) -> count + 1)
            .apply(sum -> set(counter, sum));

        //Creates a task that reads from the counter, and then prints
        //the result
        Task<String> print =
          read(counter)
            .bind(sum -> println("The sum is " + sum));

        //Starts the source and waits for completion
        dbSource.toTask().execute();

        //Prints out the value of the counter
        print.execute();
    }
}
