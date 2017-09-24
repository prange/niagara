package org.kantega.niagara;

import fj.Unit;

public interface SourceListener<A>  {

    Task<Unit> handle(A a);

}
