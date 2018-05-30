package org.kantega.niagara.task;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Util {

    static <A> Consumer<A> complete(CompletableFuture<A> cf){
        return a->cf.complete(a);
    }
}
