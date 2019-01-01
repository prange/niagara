package org.kantega.niagara.task


import arrow.core.Try
import java.time.Duration
import java.time.Instant
import java.util.concurrent.*

class TaskExecutor(
        internal val scheduledExecutorService: ScheduledExecutorService,
        internal val pool: ForkJoinPool,
        internal val defaultHandler: (Throwable) -> Task<Unit>) {

    fun <A> eval(task: Task<A>) {
        enqueue(
                TaskContext(this),
                task,
                { aTry -> aTry.fold({ Try { eval(defaultHandler(it)); it } }, { Try.just(it) }) })
    }

    fun <A> enqueue(tc: TaskContext, t: Task<A>, continuation: (Try<A>) -> Unit) {
        pool.submit {
            try {
                t.perform(tc, continuation)
            } catch (e: Throwable) {
                continuation(Try.raise(e))
            }
        }
    }

    fun <A> enqueueStage(tc: TaskContext, t: Task<A>, continuation: (Try<A>) -> Unit): CompletableFuture<Unit> {
        val cf = CompletableFuture<Unit>()
        pool.submit {
            try {
                t.perform(tc, continuation)
                cf.complete(Unit)
            } catch (e: Throwable) {
                continuation(Try.raise(e))
            }
        }
        return cf
    }

    fun <A> schedule(tc: TaskContext, t: Task<A>, continuation: (Try<A>) -> Unit, at: Instant) {
        val d = Duration.between(Instant.now(), at)
        if (d.isNegative)
            enqueue(tc, t, continuation)
        else
            scheduledExecutorService.schedule({ enqueue(tc, t, continuation) }, d.toMillis(), TimeUnit.MILLISECONDS)
    }


    fun shutdown() {
        try {
            scheduledExecutorService.awaitTermination(1, TimeUnit.DAYS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        try {
            pool.awaitTermination(1, TimeUnit.DAYS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    companion object {

        var defaultExecutor =
                TaskExecutor(
                        Executors.newSingleThreadScheduledExecutor(),
                        ForkJoinPool.commonPool()
                ) { t -> Task { t.printStackTrace() } }
    }

}
