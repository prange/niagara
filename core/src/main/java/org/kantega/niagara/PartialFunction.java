package org.kantega.niagara;

import java.util.Optional;
import java.util.function.Function;

public interface PartialFunction<A,B> {

    Optional<B> apply(A a);

    default Function<A,B> withDefault(B b){
        return a->PartialFunction.this.apply(a).orElse(b);
    }

    default Function<A,B> orElse(Function<A,B> elseFunction){
        return a->apply(a).orElse(elseFunction.apply(a));
    }

    static <A,B,AA extends A> PartialFunction<A,B> onType(Class<AA> tpe, Function<AA,B> f){
        return a -> tpe.isInstance(a) ? Optional.ofNullable(f.apply(tpe.cast(a))) : Optional.empty();
    }

}
