package org.kantega.niagara.task

import arrow.core.Try
import java.time.Instant
import java.util.concurrent.CompletableFuture


class TaskContext(private val rt: TaskExecutor) {

    var isInterrupted = false
        private set
    private var children: Pair<TaskContext, TaskContext>? = null

    fun interrupt() {
        isInterrupted = true
        if (children != null) {
            children!!.first.isInterrupted = true
            children!!.second.isInterrupted = true
        }
    }

    fun <A> enqueue(t: Task<A>, continuation: (Try<A>)->Unit) {
        rt.enqueue(this, t) { aTry ->
            if (!isInterrupted || aTry.isFailure())
            //Errors are always passed
                continuation(aTry)
            else
                continuation(Try.raise(InterruptedException("The task " + t.toString() + " was interrupted")))
        }
    }

    fun <A> enqueueStage(t: Task<A>, continuation: (Try<A>)->Unit): CompletableFuture<Unit> {
        return rt.enqueueStage(this, t, continuation)
    }

    fun <A> schedule(t: Task<A>, continuation: (Try<A>)->Unit, i: Instant) {
        rt.schedule(this, t, continuation, i)
    }

    fun branch(): Pair<TaskContext, TaskContext> {
        children = Pair(TaskContext(rt), TaskContext(rt))
        return children!!
    }
}



