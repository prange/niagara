package org.kantega.niagara;

import java.time.Duration;
import java.util.Arrays;

public class Sources {

    public static final Duration defaultTimeout = Duration.ofSeconds(10);


    public static <A> Source<A> nil() {
        return (closer, handler) -> Eventually.value(Source.ended());
    }

    public static <A> Source<A> value(A a) {
        return (closer, handler) ->
          handler.handle(a).execute().map(u -> Source.ended());
    }

    @SafeVarargs
    public static <A> Source<A> values(A... as) {
        return iterableBlock(Arrays.asList(as));
    }

    public static <A> Source<A> iterableBlock(Iterable<A> a) {
        return value(a).flatten(i -> i);
    }

}
