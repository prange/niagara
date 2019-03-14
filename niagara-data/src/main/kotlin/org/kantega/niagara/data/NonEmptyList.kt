package org.kantega.niagara.data

import io.vavr.collection.List

data class NonEmptyList<A>(val head:A,val tail:List<A>){
    fun toList():List<A> =
      List.ofAll(tail).prepend(head)

    operator fun plus(other:NonEmptyList<A>) =
      NonEmptyList(head,other.toList().prependAll(tail))

    companion object {
        fun <A> of(a:A) = NonEmptyList(a,List.empty<A>())

        fun <A> of(a:A,vararg rest:A) = NonEmptyList(a,List.ofAll<A>(rest.asIterable()))
    }
}