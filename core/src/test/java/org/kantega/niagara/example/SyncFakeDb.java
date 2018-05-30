package org.kantega.niagara.example;

import fj.data.Stream;
import org.kantega.niagara.Plan;
import org.kantega.niagara.Plans;

import java.util.Random;

public class SyncFakeDb {

    private Random r = new Random();

    public Plan<String> query() {
        System.out.println("Faking open sync database");

        Stream<String> values = Stream.range(0, 1000000).map(n -> n + " " + r.nextLong());

        System.out.println("Faking close sync database");

        return Plans.iterable(values);
    }
}
