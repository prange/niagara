package org.kantega.niagara.example;

import fj.Unit;
import org.kantega.niagara.Source;
import org.kantega.niagara.Sources;
import org.kantega.niagara.Task;

import java.time.Duration;
import java.util.Arrays;

public class Example1 {

    public static void main(String[] args) {

        Source<String> strings1 =
          Sources.values("one", "two", "three");

        Task<Unit> streamTask =
          strings1
          .apply(Example1::println)
          .flatten(l-> Arrays.asList(l.split("")))
          .onClose(println("Closing flatten").toUnit())
          .apply(Example1::println)
          .toTask();

        streamTask.execute().await(Duration.ofSeconds(4));
    }

    static Task<String> println(String line) {
        return Task.call(() -> {
            System.out.println(line);
            return line;
        });
    }

}
