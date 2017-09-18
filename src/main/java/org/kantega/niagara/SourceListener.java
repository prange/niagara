package org.kantega.niagara;

import fj.F;

public interface SourceListener<A> extends F<A, Eventually<Source.Result>> {

    Eventually<Source.Result> handle(A a);

    default Eventually<Source.Result> f(A a) {
        return handle(a);
    }

}
