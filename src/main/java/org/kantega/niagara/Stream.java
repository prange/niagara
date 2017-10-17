package org.kantega.niagara;

public interface Stream<A,B> {

    Source<B> apply(Source<A> input);

}
