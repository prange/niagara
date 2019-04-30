package org.kantega.niagara.eff

import io.vavr.collection.List
import io.vavr.kotlin.component1
import io.vavr.kotlin.component2
import org.jctools.queues.MessagePassingQueue
import org.kantega.niagara.data.P2
import org.kantega.niagara.data.p

fun <A> enqueue(q: MessagePassingQueue<A>): Sink<A> = { a ->
    Task.exec {
        q.relaxedOffer(a)
        Unit
    }
}

fun <A, B> interpret(source: Source<A>): Task<P2<List<A>, Source<A>>> {
    return when (source) {
        is Done<A>        -> Task.just(p(List.empty<A>(), Source.nil()))
        is BoundSource<A> -> interpret<A, B>(source.source).map { (output, next) -> p(output, source.f(next)) }
        else              -> Task.just(p(List.empty<A>(), Source.nil()))
    }
}
