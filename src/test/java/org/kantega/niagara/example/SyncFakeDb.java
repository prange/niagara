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
    public Eventually<Closed> open(Eventually<Stop> stop, SourceListener<String> f) {
        System.out.println("Faking open sync database");

        Stream.range(0, 1000000).foreachDoEffect(n -> {
            String randString = n + " " + r.nextLong();

            f.handle(randString);

        });

        System.out.println("Faking close sync database");

        return Eventually.value(Source.ended());
    }
}
