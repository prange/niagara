package org.kantega.niagara.blocks;

import fj.function.Try0;
import fj.function.TryEffect1;
import org.kantega.niagara.op.Scope;

public class TryConsumeBlock<A> implements Block<A> {

    final TryEffect1<A, Exception> tryConsumer;
    final Scope scope;
    final Block<A> next;

    public TryConsumeBlock(TryEffect1<A, Exception> tryConsumer, Scope scope, Block<A> next) {
        this.tryConsumer = tryConsumer;
        this.scope = scope;
        this.next = next;
    }


    @Override
    public void run(A input) {
        try {
            tryConsumer.f(input);
            next.run(input);
        } catch (Exception e) {
            scope.failure(e);
        }
    }
}
