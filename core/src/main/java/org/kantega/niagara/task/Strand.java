package org.kantega.niagara.task;

import fj.Unit;

public interface Strand<A> {

    Task<Unit> interrupt();

    Task<A> attach();
}
