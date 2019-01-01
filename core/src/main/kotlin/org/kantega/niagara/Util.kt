package org.kantega.niagara

import arrow.core.None
import arrow.core.Option
import arrow.core.Some

typealias Semigroup<A> = (A, A) -> A

data class OptionSemigroup<A>(val asg:Semigroup<A>) : Semigroup<Option<A>> {
    override fun invoke(p1: Option<A>, p2: Option<A>): Option<A> =
        p1.fold( {p2.fold( {None},::Some)},{ p -> p2.fold({Some(p)},{pp->Some(asg(p,pp))})})

}

typealias Zero<A> = () -> A

interface Monoid<A> {
    fun add(one: A, other: A): A
    fun zero(): A

    companion object {
        fun <A> invoke(sg: Semigroup<A>, z: Zero<A>): Monoid<A> = object : Monoid<A> {
            override fun add(one: A, other: A): A =
                    sg(one, other)

            override fun zero(): A = z()

        }
    }
}

