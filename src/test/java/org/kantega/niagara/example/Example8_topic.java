package org.kantega.niagara.example;

import org.kantega.niagara.Source;
import org.kantega.niagara.Task;
import org.kantega.niagara.exchange.Topic;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.kantega.niagara.example.Utils.println;
import static org.kantega.niagara.example.Utils.read;

public class Example8_topic {

    public static void main(String[] args) {

        AtomicLong counter =
          new AtomicLong();

        Topic<String> topicA =
          new Topic<>();

        Task<String> print =
          read(counter)
            .flatMap(sum -> println("The sum is " + sum));


        Source<String> topicASource =
          topicA
            .subscribe()
            .apply(Utils::println)
            .onClose(print.toUnit());

        topicASource.toTask().execute();

        topicA.publish("First").execute();
        topicA.publish("Second").execute();
        topicA.publish("Third").execute().await(Duration.ofSeconds(10));
    }
}
