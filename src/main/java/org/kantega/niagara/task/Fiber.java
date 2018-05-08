package org.kantega.niagara.task;

import fj.Unit;

public interface Fiber<A> {

    Action<Unit> interrupt();

    Action<A> attach();


}
