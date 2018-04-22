package org.kantega.niagara.example;

import org.kantega.niagara.Eval;
import org.kantega.niagara.Plan;
import org.kantega.niagara.Plans;
import org.kantega.niagara.Source;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class Example1 {

    public static void main(String[] args) {

        AtomicInteger counter = new AtomicInteger();


        Source.emit(1, 2, 3,4,5,6)
          .map(n -> n + 1)
          .repeat()
          .takeWhile(n -> n < 5)
          .repeat()
          .flatMap(n -> Arrays.asList(n, n))
          .takeWhile(n ->
            counter.incrementAndGet() < 40)
          .eval(Example1::println)
          .build()
          .run();
    }

    static <A> Eval<A> println(A line) {
        return Eval.call(() -> {
            System.out.println(line);
            return line;
        });
    }

}
