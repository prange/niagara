package org.kantega.niagara.data

import io.vavr.*

import io.vavr.kotlin.tuple

typealias P0 = Tuple0
typealias P1<A> = Tuple1<A>
typealias P2<A,B> = Tuple2<A,B>
typealias P3<A,B,C> = Tuple3<A,B,C>
typealias P4<A,B,C,D> = Tuple4<A,B,C,D>
typealias P5<A,B,C,D,E> = Tuple5<A,B,C,D,E>
typealias P6<A,B,C,D,E,F> = Tuple6<A,B,C,D,E,F>

fun <A> p(a:A) = tuple(a)
fun <A,B> p(a:A,b:B) = tuple(a,b)
fun <A,B,C> p(a:A,b:B,c:C) = tuple(a,b,c)
fun <A,B,C,D> p(a:A,b:B,c:C,d:D) = tuple(a,b,c,d)
fun <A,B,C,D,E> p(a:A,b:B,c:C,d:D,e:E) = tuple(a,b,c,d,e)
fun <A,B,C,D,E,F> p(a:A,b:B,c:C,d:D,e:E,f:F) = tuple(a,b,c,d,e,f)

fun <A,B,C> Tuple2<A,Tuple2<B,C>>.flatten():Tuple3<A,B,C> =
  Tuple.of(_1,_2._1,_2._2)