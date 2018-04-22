package org.kantega.niagara.state;

import org.kantega.niagara.Try;

import java.util.function.Consumer;

public interface Stream<O,R> {

    Step<O> stepper(Consumer<Try<R>> next);

}
