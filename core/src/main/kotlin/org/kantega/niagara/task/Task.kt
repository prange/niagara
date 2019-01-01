package org.kantega.niagara.task

import arrow.core.Either
import arrow.core.Try
import arrow.data.ListK
import fj.data.List
import org.kantega.niagara.Eval
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean


interface Task<out A> {

    fun perform(rt: TaskContext, continuation: (Try<A>) -> Unit)

    fun toUnit(): Task<Unit> {
        return map { _ -> Unit }
    }

    infix fun <B> map(f: (A) -> B): Task<B> {
        return Bind(
                this
        ) { aTry -> aTry.fold({ Fail<B>(it) }, { a -> just(f(a)) }) }
    }

    infix fun <B> flatMap(f: (A) -> Task<B>): Task<B> {
        return bind { aTry -> aTry.fold({ Fail<B>(it) }, { f(it) }) }
    }

    infix fun <B> then(next: Task<B>): Task<B> {
        return bind { aTry -> aTry.fold({ Fail<B>(it) }, { _ -> next }) }
    }

    fun <B> bind(f: (Try<A>) -> Task<B>): Task<B> {
        return Bind(this, f)
    }

    fun delay(duration: Duration): Task<A> {
        return Delayed(Either.right(duration), this)
    }

    fun delay(instant: Instant): Task<A> {
        return Delayed(Either.left(instant), this)
    }

    fun onFinish(cleanup: Task<Unit>): Task<A> {
        return Onfinish(this, cleanup)
    }

    // *** Implementations ***

    data class Bind<A, B>(internal val action: Task<A>, internal val bindFunction: (Try<A>) -> Task<B>) : Task<B> {


        override fun toString(): String {
            return "Bind{" +
                    "action=" + action +
                    ", bindFunction=" + bindFunction +
                    '}'.toString()
        }

        override fun perform(rt: TaskContext, continuation: (Try<B>) -> Unit) {
            val cont = bindFunction//prevent capturing of surrounding object
            action.perform(rt,
                    { aTry -> rt.enqueue(cont(aTry), continuation) }
            )
        }
    }

    data class Fail<A>(internal val t: Throwable) : Task<A> {


        override fun toString(): String {
            return "Fail{" +
                    t +
                    '}'.toString()
        }

        override fun perform(rt: TaskContext, continuation: (Try<A>) -> Unit) {
            continuation(Try.raise(t))
        }

        override fun <B> map(f: (A) -> B): Task<B> =
             this as Task<B>


    }

    data class Pure<A>(internal val value: A) : Task<A> {


        override fun toString(): String {
            return "Pure{" +
                    value +
                    '}'.toString()
        }

        override fun perform(rt: TaskContext, continuation: (Try<A>) -> Unit) {
            continuation(Try.just(value))
        }

        override fun <B> map(f: (A) -> B): Task<B> {
            return just(f(value))
        }
    }


    data class Fork<A, B>(internal val left: Task<A>, internal val right: Task<B>) : Task<Unit> {


        override fun toString(): String {
            return "Fork{" +
                    left +
                    ", " + right +
                    '}'.toString()
        }

        override fun perform(rt: TaskContext, continuation: (Try<Unit>) -> Unit) {
            rt.enqueue(left, { _ -> })
            rt.enqueue(right, { _ -> })
            continuation(Try { Unit })
        }
    }

    data class Par<A, B, C>(internal val left: Task<A>, internal val right: Task<B>, // (Try a, Fiber b) V (Fiber a, Try b) -> Action c
                            internal val handler: (Either<Pair<Try<A>, Strand<B>>, Pair<Strand<A>, Try<B>>>) -> Task<C>) : Task<C> {


        override fun toString(): String {
            return "Par{" +
                    left +
                    ", " + right +
                    ", handler=" + handler +
                    '}'.toString()
        }

        override fun perform(rt: TaskContext, continuation: (Try<C>) -> Unit) {
            val leftC = Canceable(left)
            val rightC = Canceable(right)
            val gate = Gate(handler, leftC, rightC, rt, continuation)
            val branch = rt.branch()
            branch.first.enqueue(leftC, { gate.left(it) })
            branch.second.enqueue(rightC, { gate.right(it) })
        }
    }

    data class Gate<A, B, C>(// (Try a, Fiber b) \/ (Fiber a, Try b) -> Action c
            private val handler: (Either<Pair<Try<A>, Strand<B>>, Pair<Strand<A>, Try<B>>>) -> Task<C>, private val aStrand: Strand<A>, private val bStrand: Strand<B>, private val rt: TaskContext, private val cont: (Try<C>) -> Unit) {
        private val wasRun = AtomicBoolean(false)


        fun left(aTry: Try<A>) {
            if (!wasRun.getAndSet(true)) {
                val cTask = handler(Either.left(Pair(aTry, bStrand)))
                cTask.perform(rt, cont)
            }
        }

        fun right(bTry: Try<B>) {
            if (!wasRun.getAndSet(true)) {
                val cTask = handler(Either.right(Pair(aStrand, bTry)))
                cTask.perform(rt, cont)
            }
        }
    }

    data class SyncrEffect<A>(internal val block: Eval<A>) : Task<A> {


        override fun toString(): String {
            return "SyncrEffect{" +
                    block +
                    '}'.toString()
        }

        override fun perform(rt: TaskContext, continuation: (Try<A>) -> Unit) {
            continuation(block.eval())
        }
    }

    data class Callback<A>(internal val future: CompletableFuture<Try<A>>) : Task<A> {


        override fun toString(): String {
            return "Callback{" +
                    future +
                    '}'.toString()
        }

        override fun perform(rt: TaskContext, continuation: (Try<A>) -> Unit) {
            future.thenAccept { continuation(it) }
        }
    }

    data class Delayed<A>(internal val instantOrDelay: Either<Instant, Duration>, internal val delayedAction: Task<A>) : Task<A> {


        override fun toString(): String {
            return "Delayed{" +
                    instantOrDelay +
                    ", " + delayedAction +
                    '}'.toString()
        }

        override fun perform(rt: TaskContext, continuation: (Try<A>) -> Unit) {
            rt.schedule(
                    delayedAction,
                    continuation,
                    instantOrDelay.fold({ i -> i }, { d -> Instant.now().plus(d) }))
        }
    }

    data class Canceable<A>(val task: Task<A>) : Task<A>, Strand<A> {
        val cancel = CompletableFuture<Boolean>()
        val callback = CompletableFuture<Try<A>>()

        override fun perform(rt: TaskContext, continuation: (Try<A>) -> Unit) {
            cancel.thenAccept { _ -> rt.interrupt() }
            task.perform(rt, { aTry ->
                callback.complete(aTry)
                continuation(aTry)
            })
        }

        override fun interrupt(): Task<Unit> {
            val i = Interrupt(cancel)
            return Task({ i.interrupt() })
        }

        override fun attach(): Task<A> {
            return Task.Callback(callback)
        }

        override fun toString(): String {
            return "Canceable{" +
                    task +
                    ", " + cancel +
                    ", " + callback +
                    '}'.toString()
        }

        internal class Interrupt(val c: CompletableFuture<Boolean>) {

            fun interrupt() {
                c.complete(true)
            }

        }
    }

    data class Onfinish<A>(internal val wrappedTask: Task<A>, internal val cleanupTask: Task<Unit>) : Task<A> {

        override fun perform(rt: TaskContext, continuation: (Try<A>) -> Unit) {
            wrappedTask.perform(rt) { aTry ->
                rt.enqueue(cleanupTask) {/*ignore output from cleanup*/ _ -> }
                continuation(aTry)
            }
        }
    }

    companion object {


        operator fun <A> invoke(s: () -> A): Task<A> =
                invoke(Eval.call(s))


        operator fun <A> invoke(a: Eval<A>): Task<A> =
                SyncrEffect(a)

        fun <A, B, C> bind(
                first: ()-> Task<A>,
                second: (A) -> Task<B>,
                third: (A, B) -> Task<C>
        ): Task<C> =
                first().flatMap { a -> second(a).flatMap { b -> third(a, b) } }

        fun <A> just(value: A): Task<A> =
                Pure(value)


        fun <B> fail(t: Throwable): Task<B> =
                Fail(t)


        fun <A, B> fork(aAction: Task<A>, bAction: Task<B>): Task<Unit> =
                Fork(aAction, bAction)


        fun <A, B, C> par(
                a: Task<A>,
                b: Task<B>,
                handler: (Either<Pair<Try<A>, Strand<B>>, Pair<Strand<A>, Try<B>>>) -> Task<C>): Task<C> =
                Par(a, b, handler)


        fun <A, B, C> join(aAction: Task<A>, bAction: Task<B>, joiner: (A, B) -> C): Task<C> =
                par(aAction, bAction, { p2P2Either ->
                    p2P2Either.fold(
                            { leftPair ->
                                val lResult = leftPair.first
                                val rFiber = leftPair.second
                                lResult.fold(
                                        { t -> rFiber.interrupt().then(fail<C>(t)) },
                                        { a -> rFiber.attach().map({ b -> joiner(a, b) }) }
                                )
                            },
                            { rigthPair ->
                                val rResult = rigthPair.second
                                val lFiber = rigthPair.first
                                rResult.fold(
                                        { t -> lFiber.interrupt().then(fail(t)) },
                                        { b -> lFiber.attach().map({ a -> joiner(a, b) }) }
                                )
                            }
                    )
                })


        fun <A> callback(handler: ((Try<A>) -> Unit) -> Unit): Task<A> {
            val cf = CompletableFuture<Try<A>>()
            handler({ cf.complete(it) })
            return Callback(cf)
        }
    }
}

fun <A> List<Task<A>>.sequence(): Task<List<A>> =
        if (this@sequence.isEmpty)
            Task { List.nil<A>() }
        else
            this@sequence.head().flatMap { a -> this@sequence.tail().sequence().map { list -> list.cons(a) } }
