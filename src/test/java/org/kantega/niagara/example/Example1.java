package org.kantega.niagara.example;

import org.kantega.niagara.Eval;
import org.kantega.niagara.Plan;
import org.kantega.niagara.Plans;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class Example1 {

    public static void main(String[] args) {

        Plan<Integer> strings1 =
          Plans.emit(1, 2, 3);

        AtomicInteger counter = new AtomicInteger();


        strings1
          .map(n -> n + 1)
          .eval(Example1::println)
          .repeat()
          .takeWhile(n -> n > 5)
          // .repeat()
           .flatMap(n-> Arrays.asList(n,n,n,n,n))
           .eval(Example1::println)
          .takeWhile(n -> counter.incrementAndGet() > 20)
          .run();
    }

    static <A> Eval<A> println(A line) {
        return Eval.call(() -> {
            System.out.println(line);
            return line;
        });
    }

}
