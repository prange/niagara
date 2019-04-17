package org.kantega.niagara.data

import io.vavr.collection.List

infix fun <A> List<A>.isPrefixOf(other: List<A>): Boolean =
  isPrefixOf(equals(), other)

fun <A> List<A>.isPrefixOf(eq: Equals<A>, other: List<A>): Boolean =
  headOption()
    .fold(
      { true },
      { a ->
          other
            .headOption()
            .map({ b -> if (eq(a, b)) this.tail().isPrefixOf(other.tailOption().getOrElse(List.empty())) else false })
            .getOrElse(false)
      }
    )



