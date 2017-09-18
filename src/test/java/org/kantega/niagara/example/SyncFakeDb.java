package org.kantega.niagara.example;

import fj.data.Stream;
import org.kantega.niagara.Eventually;
import org.kantega.niagara.Source;
import org.kantega.niagara.SourceListener;

import java.time.Duration;
import java.util.Random;

public class SyncFakeDb implements Source<String> {

    private Random r = new Random();

    @Override
    public Eventually<Source.Running> open(SourceListener<String> f) {
        System.out.println("Faking open database");

        Stream.range(0, 10000000).foreachDoEffect(n -> {
            String randString = n + " " + r.nextLong();

            f.handle(randString).await(Duration.ofSeconds(2));

        });

        System.out.println("Faking close database");

        return Eventually.value(new Source.Running(Eventually.value(Source.Result.ack)));
    }
}
