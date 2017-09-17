package org.kantega.niagara;

import java.util.Arrays;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class Blocks {

    public static <A> Block<A> value(A a) {
        return handler -> {
            Block.Result r = handler.f(a);
            return completedFuture(new Block.Running(completedFuture(r)));
        };
    }

    @SafeVarargs
    public static <A> Block<A> values(A ... as){
        return iterableBlock(Arrays.asList(as));
    }

    public static <A> Block<A> iterableBlock(Iterable<A> a) {
        return value(a).flatten(i -> i);
    }

}
