package org.kantega.niagara;

import fj.Unit;

public interface Sink<A> {

    public Task<Unit> consume(A a);

}
