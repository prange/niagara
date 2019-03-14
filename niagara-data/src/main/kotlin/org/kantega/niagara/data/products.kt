package org.kantega.niagara.data

import io.vavr.Tuple1
import io.vavr.Tuple2
import io.vavr.Tuple3

import io.vavr.kotlin.tuple

typealias P1<A> = Tuple1<A>
typealias P2<A,B> = Tuple2<A,B>
typealias P3<A,B,C> = Tuple3<A,B,C>

fun <A> p(a:A) = tuple(a)
fun <A,B> p(a:A,b:B) = tuple(a,b)
fun <A,B,C> p(a:A,b:B,c:C) = tuple(a,b,c)