package org.kantega.niagara.state;

import java.util.function.Function;

public interface Next<O, R> {

    void next(Instruction<O, R> next);

    default <R2> Next<O, R2> comap(Function<Instruction<O, R2>, Instruction<O, R>> function) {
        return next -> Next.this.next(function.apply(next));
    }

}
