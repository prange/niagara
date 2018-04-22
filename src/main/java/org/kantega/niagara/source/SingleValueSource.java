package org.kantega.niagara.source;

import org.kantega.niagara.Source;
import org.kantega.niagara.Emitter;
import org.kantega.niagara.sink.Sink;

public class SingleValueSource<O> implements Source<O> {
    private final O value;

    public SingleValueSource(O value) {
        this.value = value;
    }

    @Override
    public Emitter build(Sink<O> emit, Done<O> done) {
        return () -> {
            emit.accept(value);
            done.done(Source.nil());
            return true;
        };
    }
}
