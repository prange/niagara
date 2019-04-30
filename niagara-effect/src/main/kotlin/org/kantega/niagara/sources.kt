package org.kantega.niagara

import org.jctools.queues.MessagePassingQueue

fun <A> enqueue(q: MessagePassingQueue<A>): Sink<A> = { a ->
    Task.exec {
        q.relaxedOffer(a)
        Unit
    }
}