package org.kantega.niagara.example;

import org.kantega.niagara.Block;
import org.kantega.niagara.Blocks;
import org.kantega.niagara.Task;

import java.util.Arrays;

public class Example1 {

    public static void main(String[] args) {

        Block<String> strings1 =
          Blocks.values("one", "two", "three");

        strings1
          .apply(Example1::println)
          .flatten(l-> Arrays.asList(l.split("")))
          .onClose(println("Closing flatten").toUnit())
          .apply(Example1::println)
          .run();
    }

    static Task<String> println(String line) {
        return Task.call(() -> {
            System.out.println(line);
            return line;
        });
    }

}
