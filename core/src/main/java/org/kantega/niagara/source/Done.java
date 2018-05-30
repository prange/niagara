package org.kantega.niagara.source;

import org.kantega.niagara.Source;

import java.util.function.Function;

public interface Done<A> {

    void done(Source<A> remainder);

    static <A> Done<A> noOp(){
        return a->{};
    }

    default <B> Done<B> comap(Function<Source<B>,Source<A>> f){
        return new CoMappedDone<>(f,this);
    }

    class CoMappedDone<A,B> implements Done<A>{
        final Function<Source<A>,Source<B>> function;
        final Done<B> next;

        public CoMappedDone(Function<Source<A>,Source<B>> function, Done<B> next) {
            this.function = function;
            this.next = next;
        }

        @Override
        public void done(Source<A> remainder) {
            next.done(function.apply(remainder));
        }
    }

}
