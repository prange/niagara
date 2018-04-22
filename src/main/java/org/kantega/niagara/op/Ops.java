package org.kantega.niagara.op;

import org.kantega.niagara.Source;

public interface Ops<O> {


    <O2> Ops<O2> append(StageOp<O, O2> next);

    static <O> Ops<O> ops(Source<O> source) {
        return new RootOp<>(source);
    }

    class RootOp<A> implements Ops<A> {

        final Source<A> source;

        public RootOp(Source<A> source) {
            this.source = source;
        }

        @Override
        public <O2> Ops<O2> append(StageOp<A, O2> next) {
            return new OpChain<>(this, next);
        }
    }

    class OpChain<A, O> implements Ops<O> {

        final Ops<A> source;
        final StageOp<A, O> chain;

        public OpChain(Ops<A> source, StageOp<A, O> chain) {
            this.source = source;
            this.chain = chain;
        }

        @Override
        public <O2> Ops<O2> append(StageOp<O, O2> next) {
            return new OpChain<>(source, chain.append(next));
        }
    }

}
