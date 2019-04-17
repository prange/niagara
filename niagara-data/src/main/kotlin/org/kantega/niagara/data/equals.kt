package org.kantega.niagara.data

typealias Equals<A> = (A, A) -> Boolean

fun <A> equals(): Equals<A> =
  { a, b -> a == b }