package org.kantega.niagara.state;

import org.kantega.niagara.sink.Sink;
import org.kantega.niagara.thread.WaitStrategy;

public interface Scope<O> {

    Sink<O> sink();

    WaitStrategy waitStrategy();

    void setNext(Instruction<O,?> next);

}
