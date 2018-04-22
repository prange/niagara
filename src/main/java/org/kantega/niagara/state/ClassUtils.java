package org.kantega.niagara.state;

import java.util.Optional;

import static fj.data.Option.none;
import static fj.data.Option.some;

public class ClassUtils {

    public static <A> Optional<A> cast(Object a, Class<A> tpe) {
        return tpe.isInstance(a) ? Optional.of(tpe.cast(a)) : Optional.empty();
    }


}
