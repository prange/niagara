package org.kantega.niagara.example;

import fj.Unit;
import org.kantega.niagara.Eval;
import org.kantega.niagara.Plan;
import org.kantega.niagara.Plans;
import org.kantega.niagara.Source;
import org.kantega.niagara.task.Task;
import org.kantega.niagara.task.TaskExecutor;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class Example1 {

    public static void main(String[] args) throws InterruptedException {

        var te = new TaskExecutor();
        var counter = new AtomicInteger();


        Task<Unit> task =
          Plans.emit(1, 2, 3, 4, 5, 6)
            .map(n -> n + 1)
            .repeat()
            .takeWhile(n -> n < 5)
            .repeat()
            .flatMap(n -> Arrays.asList(n, n))
            .takeWhile(n ->
              counter.incrementAndGet() < 20)
            .eval(Example1::println)
            .compile();

        te.eval(task);

        Thread.sleep(10000);
    }

    static <A> Eval<A> println(A line) {
        return Eval.call(() -> {
            System.out.println(line);
            return line;
        });
    }

}
