package org.kantega.niagara.blocks;

import org.kantega.niagara.Eval;

import java.util.function.Function;

public class EvalBlock<A, B> implements Block<A> {

    final Function<A, Eval<B>> f;
    final Block<B> next;

    public EvalBlock(Function<A, Eval<B>> f, Block<B> next) {
        this.f = f;
        this.next = next;
    }

    @Override
    public void run(A input) {

        try {
            next.run(f.apply(input).evaluate().orThrow());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
