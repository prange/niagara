package org.kantega.niagara.op;

import org.kantega.niagara.state.Scope;
import org.kantega.niagara.sink.TakeWhileSink;

import java.util.function.Predicate;

public class TakeWhileOp<A> implements KeepTypeOp<A> {

    final Predicate<A> pred;

    public TakeWhileOp(Predicate<A> pred) {
        this.pred = pred;
    }

    @Override
    public Scope<A> build(Scope<A> input) {
        return Scope.scope(
          new TakeWhileSink<>(pred, input.consumer, d->{
              input.halt();
              input.done.done(d);
          }),
          input.done.comap(this));
    }


}
