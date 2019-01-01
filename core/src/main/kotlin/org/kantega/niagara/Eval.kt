package org.kantega.niagara

import arrow.core.Try

/**
 * Represents an evaluation of a value. The value can be eager or lazy. A lazy
 * Eval may
 * be evaluated for every call, or be memoized. It might be computed in another thread
 * but the call is blocking.
 *
 * @param <A> The type of the value that is the result of this evaluation.
</A> */
interface Eval<A> {

    fun eval(): Try<A>

    companion object {

        fun <A> call(supplier: () -> A): Eval<A> =
                EvalSupplier(supplier)


        fun <A> fail(t: Throwable): Eval<A> =
                FailedEval(t)

    }

    data class EvalSupplier<A>(val supplier: () -> A) : Eval<A> {
        override fun eval(): Try<A> =
            Try { supplier.invoke() }


    }

    data class FailedEval<A>(val t: Throwable) : Eval<A> {
        override fun eval(): Try<A> =
                Try.raise(t)

    }
}
