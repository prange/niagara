package org.kantega.niagara;

import fj.function.Effect1;

public interface SourceListener<A> extends Effect1<A> {

    void handle(A a);

    default void f(A a) {

    }

}
