package org.kantega.niagara.source;

import org.kantega.niagara.Emitter;
import org.kantega.niagara.Source;
import org.kantega.niagara.state.Scope;

public class NilSource<O> implements Source<O> {
    @Override
    public Emitter build(Scope<O> sink) {
        return () -> {
            sink.done.done(this);
            return false;
        };
    }

    @Override
    public boolean isNil() {
        return true;
    }
}
