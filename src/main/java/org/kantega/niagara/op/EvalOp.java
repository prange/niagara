package org.kantega.niagara.op;

import org.kantega.niagara.Eval;
import org.kantega.niagara.Source;
import org.kantega.niagara.sink.EvaluatingSink;

import java.util.function.Function;

public class EvalOp<A, B> implements StageOp<A, B> {

    final Function<A, Eval<B>> evalFunction;

    public EvalOp(Function<A, Eval<B>> evalFunction) {
        this.evalFunction = evalFunction;
    }


    @Override
    public Source<B> apply0(Source<A> input) {
        return (emit, done) -> input.build(
          new EvaluatingSink<>(evalFunction, emit, done),
          done.comap(this));
    }
}
