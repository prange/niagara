package org.kantega.niagara.eff

import io.vavr.control.Try
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService

interface Task<A> {

    fun toUnit() =
      map { Unit }

    infix fun <B> bind(f: (A) -> Task<B>): Task<B> =
      BoundTask(this, { ta -> ta.fold({ t -> fail<B>(t) }, { s -> f(s) }) })

    fun handle(f: (Throwable) -> Task<A>) =
      BoundTask(this, { ta -> ta.fold({ t -> f(t) }, { s -> just(s) }) })

    infix fun <B> map(f: (A) -> B): Task<B> =
      bind { a -> just(f(a)) }

    infix fun <B> then(next: Task<B>): Task<B> =
      bind { next }

    fun execute(executor: ScheduledExecutorService, continuation: (Try<A>) -> Unit)

    companion object {

        operator fun <A> invoke(block: () -> A):Task<A> =
          exec(block)

        operator fun <A> invoke(value:A):Task<A> =
          just(value)

        fun <A> exec(block: () -> A): Task<A> =
          EffectTask {
              Try.of(block)
          }

        fun <A> doTry(block: () -> Try<A>): Task<A> =
          EffectTask(block)

        fun <A> just(a: A): Task<A> =
          ValueTask(a)

        fun <A> fail(e: Throwable): Task<A> =
          FailedTask(e)
    }

}

data class EffectTask<A>(val effect: () -> Try<A>) : Task<A> {
    override fun execute(executor: ScheduledExecutorService, continuation: (Try<A>) -> Unit) {
        executor.execute {
            val t = try {
                effect()
            } catch (e: Throwable) {
                Try.failure<A>(e)
            }

            continuation(t)
        }
    }

    override fun <B> map(f: (A) -> B): Task<B> =
      EffectTask { effect().map(f) }
}

data class ValueTask<A>(val a: A) : Task<A> {
    override fun execute(executor: ScheduledExecutorService, continuation: (Try<A>) -> Unit) {
        continuation(Try.success(a))
    }

    override fun <B> map(f: (A) -> B): Task<B> =
      ValueTask(f(a))
}

data class FailedTask<A>(val e: Throwable) : Task<A> {
    override fun execute(executor: ScheduledExecutorService, continuation: (Try<A>) -> Unit) {
        continuation(Try.failure(e))
    }

    override fun <B> map(f: (A) -> B): Task<B> =
      FailedTask(e)
}

data class BoundTask<A, B>(val first: Task<A>, val cont: (Try<A>) -> Task<B>) : Task<B> {
    override fun execute(executor: ScheduledExecutorService, continuation: (Try<B>) -> Unit) {
        first.execute(
          executor,
          { a ->
              try {
                  cont(a).execute(executor, continuation)
              }catch (e:Exception){
                  Task.fail<B>(e).execute(executor,continuation)
              }
          })
    }
}


data class ParTask<A, B, C>(val left: Task<A>, val right: Task<B>, val join: (Try<A>, Try<B>) -> Try<C>) : Task<C> {
    override fun execute(executor: ScheduledExecutorService, continuation: (Try<C>) -> Unit) {
        val ca = CompletableFuture<Try<A>>()
        val cb = CompletableFuture<Try<B>>()

        left.execute(executor, { a -> ca.complete(a) })
        right.execute(executor, { b -> cb.complete(b) })

        ca.thenAcceptBoth(cb, { ta, tb -> continuation(join(ta, tb)) })
    }

}



