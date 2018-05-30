package org.kantega.niagara.op;

import org.kantega.niagara.Eval;
import org.kantega.niagara.sink.EvaluatingConsumer;
import org.kantega.niagara.state.Scope;

import java.util.function.Function;

public class EvalOp<A, B> implements StageOp<A, B> {

    final Function<A, Eval<B>> evalFunction;

    public EvalOp(Function<A, Eval<B>> evalFunction) {
        this.evalFunction = evalFunction;
    }


    @Override
    public Scope<A> build(Scope<B> input) {
        return Scope.scope(
          new EvaluatingConsumer<>(evalFunction, input.consumer, input.done),
          input.done.comap(this));
    }
}
