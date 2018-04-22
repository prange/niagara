package org.kantega.niagara.op;

import org.kantega.niagara.Source;

public class AppendOp<A> implements StageOp<A, A> {

    final Source<A> appended;

    public AppendOp(Source<A> appended) {
        this.appended = appended;
    }

    @Override
    public Source<A> apply0(Source<A> input) {
        return (emit, done) -> input.build(
          emit,
          done.comap(this));
    }
}
