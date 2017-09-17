package org.kantega.niagara;

import fj.P2;

public interface Mealy<A, B> {

    P2<Mealy<A, B>,B> apply(A a);

}
