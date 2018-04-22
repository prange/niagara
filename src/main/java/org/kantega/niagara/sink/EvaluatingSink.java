package org.kantega.niagara.sink;

import org.kantega.niagara.Try;
import org.kantega.niagara.Eval;
import org.kantega.niagara.Source;
import org.kantega.niagara.source.Done;

import java.util.function.Consumer;
import java.util.function.Function;

public class EvaluatingSink<O, O2> implements Sink<O> {

    final Function<O, Eval<O2>> function;
    final Consumer<O2> next;
    final Done<O2> done;

    public EvaluatingSink(Function<O, Eval<O2>> function, Consumer<O2> next, Done<O2> done) {
        this.function = function;
        this.next = next;
        this.done = done;
    }

    @Override
    public void accept(O o) {
        Try<O2> att = function.apply(o).evaluate();
        att.doEffect(t->done.done(Source.nil()), next::accept);
    }
}
