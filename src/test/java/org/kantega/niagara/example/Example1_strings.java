package org.kantega.niagara.example;

import fj.Unit;
import org.kantega.niagara.Source;
import org.kantega.niagara.Sources;
import org.kantega.niagara.Task;

import java.time.Duration;
import java.util.Arrays;

public class Example1_strings {

    public static void main(String[] args) {

        Source<String> strings1 =
          Sources.emit("one", "two", "three");

        Task<?> streamTask =
          strings1
          .apply(Example1_strings::println)
          .flatten(l-> Arrays.asList(l.split("")))
          .onClose(println("Closing flatten").toUnit())
          .apply(Example1_strings::println)
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
