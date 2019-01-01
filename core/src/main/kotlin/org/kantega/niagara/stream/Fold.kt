package org.kantega.niagara.stream

import org.jctools.queues.MessagePassingQueue
import org.kantega.niagara.task.Task
import org.kantega.niagara.task.TaskExecutor

typealias Sink<I> = (I) -> Task<Unit>
typealias Update<O, S> = (O, S) -> S

data class Accum<S, O>(val init: S, val f: Update<O, S>, val interrupt: (S) -> Boolean)

data class Next<S, O>(val sum: S, val cont: Fold<O>) {
    fun <O2> transformNext(f: (Fold<O>) -> Fold<O2>) =
            Next(sum, f(cont))
}

interface Fold<O> {

    fun <S> fold(accum: Accum<S, O>): Task<Next<S, O>>

    fun bind(f: (Fold<O>) -> Fold<O>): Fold<O> = object : Fold<O> {
        override fun <S> fold(accum: Accum<S, O>): Task<Next<S, O>> =
                this@Fold.fold(accum).flatMap { (s, r) -> f(r).fold(Accum(s, accum.f, accum.interrupt)) }
    }

    fun <O2> map(f: (O) -> O2) : Fold<O2> =
            MappedFold(this, f)

    fun <O2> flatMap(f: (O) -> Sequence<O2>) =
            FlatMappedFold(this, f)

    fun merge(other: Fold<O>): Fold<O> =
            Fold.merge(this, other)

    fun loop(): Fold<O> = object : Fold<O> {
        override fun <S> fold(accum: Accum<S, O>): Task<Next<S, O>> =
                this@Fold.fold(accum).flatMap { (s, r) ->
                    if (r.isNil())
                        Task.just(Next(s, NilFold()))
                    else
                        r.loop().fold(Accum(s, accum.f, accum.interrupt))
                }
    }

    infix fun append(next: () -> Fold<O>): Fold<O> =
            Append(this, next)

    fun isNil() =
            false

    fun sink(sink: Sink<O>, te: TaskExecutor): Fold<O> =
            ExecutingFold(this, sink, te)

    fun compile() =
            compile(Unit, { _, _ -> Unit })

    fun <S> compile(s: S, f: (O, S) -> S): Task<S> =
            compile(Accum(s, f, { _ -> false }))


    fun <S> compile(accum: Accum<S, O>): Task<S> =
            fold(accum).flatMap { (s, r) ->
                if (r.isNil())
                    Task.just(s)
                else
                    r.compile(Accum(s, accum.f, accum.interrupt))
            }


    companion object {

        fun <O> merge(one: Fold<O>, other: Fold<O>): Fold<O> =
                when {
                    one.isNil() -> other
                    other.isNil() -> one
                    else -> one.bind { l -> other.bind { r -> Fold.merge(l, r) } }
                }

        fun <O> nil() =
                NilFold<O>()

        fun <O> just(task: Task<Iterable<O>>): Fold<O> =
                PureFold(task)

        fun <O> output1(o: O): Fold<O> =
                Output1(o)

        fun <O> output(vararg os: O): Fold<O> =
                IteratorFold(os.iterator())

        fun <O> iterable(i: Iterable<O>): Fold<O> =
                IteratorFold(i.iterator())

        fun <O> queue(q: MessagePassingQueue<O>): Fold<O> =
                QueueFold(q)


    }
}

class NilFold<O> : Fold<O> {
    override fun <S> fold(accum: Accum<S, O>): Task<Next<S, O>> =
            Task.just(Next(accum.init, NilFold()))

    override fun isNil() = true

}

data class PureFold<O>(val task: Task<Iterable<O>>) : Fold<O> {
    override fun <S> fold(accum: Accum<S, O>): Task<Next<S, O>> =
            task flatMap { iter -> Fold.iterable(iter).fold(accum) }
}

data class QueueFold<O>(val queue: MessagePassingQueue<O>) : Fold<O> {
    override fun <S> fold(accum: Accum<S, O>): Task<Next<S, O>> =
            Task {
                var s = accum.init
                var o: O
                var running = true
                while (running && !accum.interrupt(s)) {
                    o = queue.relaxedPoll()
                    running = o != null
                    s = accum.f(o, s)
                }

                Next(s, QueueFold(queue))
            }
}

data class IteratorFold<O>(val iter: Iterator<O>) : Fold<O> {
    override fun <S> fold(accum: Accum<S, O>): Task<Next<S, O>> =
            Task {
                var s = accum.init
                while (iter.hasNext() && !accum.interrupt(s)) {
                    s = accum.f(iter.next(), s)
                }

                if (iter.hasNext())
                    Next(s, IteratorFold(iter))
                else
                    Next(s, NilFold())
            }
}

data class Output1<O>(val o: O) : Fold<O> {
    override fun <S> fold(accum: Accum<S, O>): Task<Next<S, O>> =
            Task { Next<S, O>(accum.f(o, accum.init), NilFold()) }
}

data class Append<O>(val first: Fold<O>, val appended: () -> Fold<O>) : Fold<O> {
    override fun <S> fold(accum: Accum<S, O>): Task<Next<S, O>> =
            first.fold(accum) map { next -> if (next.cont.isNil()) Next(next.sum, appended()) else Next(next.sum, next.cont.append(appended)) }
}

data class ExecutingFold<O>(val wrapped: Fold<O>, val sink: Sink<O>, val te: TaskExecutor) : Fold<O> {
    override fun <S> fold(accum: Accum<S, O>): Task<Next<S, O>> =
            wrapped.fold(Accum(accum.init, { o, s -> te.eval(sink(o));accum.f(o, s) }, accum.interrupt))
}

data class MappedFold<O, O2>(val wrapped: Fold<O>, val f: (O) -> O2) : Fold<O2> {
    override fun <S> fold(accum: Accum<S, O2>): Task<Next<S, O2>> =
            wrapped.fold(Accum(accum.init, { o, s -> accum.f(f(o), s) }, accum.interrupt)).map { next -> next.transformNext { fold -> fold.map(f) } }
}

data class FlatMappedFold<O, O2>(val wrapped: Fold<O>, val f: (O) -> Sequence<O2>) :  Fold<O2> {
    override fun <S> fold(accum: Accum<S, O2>): Task<Next<S, O2>> =
            wrapped.fold(Accum(accum.init, { o, s -> f(o).fold(s,{ss,o->accum.f(o,ss)})}, accum.interrupt)).map { next -> next.transformNext { fold -> fold.flatMap(f) } }
}