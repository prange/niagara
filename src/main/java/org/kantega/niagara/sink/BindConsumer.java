package org.kantega.niagara.sink;

import org.kantega.niagara.Plan;
import org.kantega.niagara.state.NoWaitScope;

import java.util.function.Consumer;
import java.util.function.Function;

public class BindConsumer<O,O2> implements Consumer<O> {

    final Consumer<O2> next;
    final Function<O,Plan<O2>> function;

    public BindConsumer(Consumer<O2> next, Function<O, Plan<O2>> function) {
        this.next = next;
        this.function = function;
    }


    @Override
    public void accept(O o) {
        function.apply(o).instruction.eval(new NoWaitScope<>(next)).complete();
    }
}
