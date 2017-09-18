package org.kantega.niagara;

import java.util.Arrays;

public class Sources {

    public static <A> Source<A> value(A a) {
        return handler -> {
            Eventually<Source.Result> r = handler.f(a);
            return Eventually.value(new Source.Running(r));
        };
    }

    @SafeVarargs
    public static <A> Source<A> values(A ... as){
        return iterableBlock(Arrays.asList(as));
    }

    public static <A> Source<A> iterableBlock(Iterable<A> a) {
        return value(a).flatten(i -> i);
    }

}
