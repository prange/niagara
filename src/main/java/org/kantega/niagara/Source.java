package org.kantega.niagara;

import org.kantega.niagara.op.StageOp;
import org.kantega.niagara.sink.Sink;
import org.kantega.niagara.source.*;

import java.util.Arrays;
import java.util.Queue;

public interface Source<O> {

    Emitter build(Sink<O> emit, Done<O> done);

    default <O2> Source<O2> append(StageOp<O,O2> op){
        return new SourceCompiler<>(this,op);
    }

    static <O> Source<O> single(O value) {
        return new SingleValueSource<>(value);
    }

    static <O> Source<O> emit(O... values) {
        return values.length == 0 ? nil() : iterable(Arrays.asList(values));
    }

    static <O> Source<O> iterable(Iterable<O> iterable) {
        return new IterableSource<>(iterable);
    }

    static <O> Source<O> nil() {
        return new NilSource<>();
    }

    static <O> Source<O> queue(Queue<O> q) {
        return new QueueSource<>(q);
    }

    default boolean isNil(){
        return this instanceof NilSource;
    }

    class SourceCompiler<O,O2> implements Source<O2>{

        final Source<O> source;
        final StageOp<O, O2> ops;

        public SourceCompiler(Source<O> source, StageOp<O, O2> ops) {
            this.source = source;
            this.ops = ops;
        }


        @Override
        public Emitter build(Sink<O2> emit, Done<O2> done) {
            return ops.apply(source).build(emit,done);
            skrive om til at build kan ta inn P2, og apply blir P2<Sink,Done> -> P2<Sink,Done>
        }

        @Override
        public <O3> Source<O3> append(StageOp<O2, O3> op) {
            return new SourceCompiler<>(source,ops.fuse(op));
        }

    }
}
