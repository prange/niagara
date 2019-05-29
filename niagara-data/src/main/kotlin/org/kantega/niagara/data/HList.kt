package org.kantega.niagara.data

import io.vavr.*


sealed class HList(val size:Int)

data class HCons<out H, out T : HList>(val head: H, val tail: T) : HList(1 + tail.size)

object HNil : HList(0)


typealias HList1<A> = HCons<A, HNil>

typealias HList2<A, B> = HCons<A, HCons<B, HNil>>
typealias HList3<A, B, C> = HCons<A, HCons<B, HCons<C, HNil>>>
typealias HList4<A, B, C, D> = HCons<A, HCons<B, HCons<C, HCons<D, HNil>>>>
typealias HList5<A, B, C, D, E> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HNil>>>>>
typealias HList6<A, B, C, D, E, F> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HNil>>>>>>
typealias HList7<A, B, C, D, E, F, G> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HNil>>>>>>>
typealias HList8<A, B, C, D, E, F, G, H> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HCons<H, HNil>>>>>>>>
typealias HList9<A, B, C, D, E, F, G, H, I> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HCons<H, HCons<I, HNil>>>>>>>>>
typealias HList10<A, B, C, D, E, F, G, H, I, J> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HCons<H, HCons<I, HCons<J, HNil>>>>>>>>>>
typealias HList11<A, B, C, D, E, F, G, H, I, J, K> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HCons<H, HCons<I, HCons<J, HCons<K, HNil>>>>>>>>>>>
typealias HList12<A, B, C, D, E, F, G, H, I, J, K, L> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HCons<H, HCons<I, HCons<J, HCons<K, HCons<L, HNil>>>>>>>>>>>>
typealias HList13<A, B, C, D, E, F, G, H, I, J, K, L, M> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HCons<H, HCons<I, HCons<J, HCons<K, HCons<L, HCons<M, HNil>>>>>>>>>>>>>
typealias HList14<A, B, C, D, E, F, G, H, I, J, K, L, M, N> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HCons<H, HCons<I, HCons<J, HCons<K, HCons<L, HCons<M, HCons<N, HNil>>>>>>>>>>>>>>
typealias HList15<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HCons<H, HCons<I, HCons<J, HCons<K, HCons<L, HCons<M, HCons<N, HCons<O, HNil>>>>>>>>>>>>>>>
typealias HList16<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HCons<H, HCons<I, HCons<J, HCons<K, HCons<L, HCons<M, HCons<N, HCons<O, HCons<P, HNil>>>>>>>>>>>>>>>>
typealias HList17<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HCons<H, HCons<I, HCons<J, HCons<K, HCons<L, HCons<M, HCons<N, HCons<O, HCons<P, HCons<Q, HNil>>>>>>>>>>>>>>>>>
typealias HList18<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HCons<H, HCons<I, HCons<J, HCons<K, HCons<L, HCons<M, HCons<N, HCons<O, HCons<P, HCons<Q, HCons<R, HNil>>>>>>>>>>>>>>>>>>
typealias HList19<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HCons<H, HCons<I, HCons<J, HCons<K, HCons<L, HCons<M, HCons<N, HCons<O, HCons<P, HCons<Q, HCons<R, HCons<S, HNil>>>>>>>>>>>>>>>>>>>
typealias HList20<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HCons<H, HCons<I, HCons<J, HCons<K, HCons<L, HCons<M, HCons<N, HCons<O, HCons<P, HCons<Q, HCons<R, HCons<S, HCons<T, HNil>>>>>>>>>>>>>>>>>>>>
typealias HList21<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HCons<H, HCons<I, HCons<J, HCons<K, HCons<L, HCons<M, HCons<N, HCons<O, HCons<P, HCons<Q, HCons<R, HCons<S, HCons<T, HCons<U, HNil>>>>>>>>>>>>>>>>>>>>>
typealias HList22<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V> = HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HCons<H, HCons<I, HCons<J, HCons<K, HCons<L, HCons<M, HCons<N, HCons<O, HCons<P, HCons<Q, HCons<R, HCons<S, HCons<T, HCons<U, HCons<V, HNil>>>>>>>>>>>>>>>>>>>>>>


fun <A> HList1<A>.tuple() =
  Tuple1(head)

fun <A, B> HList2<A, B>.tuple() =
  Tuple2(head, tail.head)

fun <A, B, C> HList3<A, B, C>.tuple() =
  Tuple3(head, tail.head, tail.tail.head)

fun <A, B, C, D> HList4<A, B, C, D>.tuple() =
  Tuple4(head, tail.head, tail.tail.head, tail.tail.tail.head)

fun <A, B, C, D, E> HList5<A, B, C, D, E>.tuple() =
  Tuple5(head, tail.head, tail.tail.head, tail.tail.tail.head, tail.tail.tail.tail.head)

fun <A, B, C, D, E, F> HList6<A, B, C, D, E, F>.tuple() =
  Tuple6(head, tail.head, tail.tail.head, tail.tail.tail.head, tail.tail.tail.tail.head, tail.tail.tail.tail.head)

fun <A, B, C, D, E, F, G> HList7<A, B, C, D, E, F, G>.tuple() =
  Tuple7(head, tail.head, tail.tail.head, tail.tail.tail.head, tail.tail.tail.tail.head, tail.tail.tail.tail.head, tail.tail.tail.tail.tail.head)

fun <A, B, C, D, E, F, G,H> HList8<A, B, C, D, E, F, G,H>.tuple() =
  Tuple8(head, tail.head, tail.tail.head, tail.tail.tail.head, tail.tail.tail.tail.head, tail.tail.tail.tail.head, tail.tail.tail.tail.tail.head,tail.tail.tail.tail.tail.tail.head)

fun <A> hList(a:A) = HCons(a,HNil)
fun <A,B> hList(a:A,b:B) = HCons(a,hList(b))
fun <A,B,C> hList(a:A,b:B,c:C) = HCons(a,hList(b,c))
fun <A,B,C,D> hList(a:A,b:B,c:C,d:D) = HCons(a,hList(b,c,d))
fun <A,B,C,D,E> hList(a:A,b:B,c:C,d:D,e:E) = HCons(a,hList(b,c,d,e))
fun <A,B,C,D,E,F> hList(a:A,b:B,c:C,d:D,e:E,f:F) = HCons(a,hList(b,c,d,e,f))
fun <A,B,C,D,E,F,G> hList(a:A,b:B,c:C,d:D,e:E,f:F,g:G) = HCons(a,hList(b,c,d,e,f,g))
fun <A,B,C,D,E,F,G,H> hList(a:A,b:B,c:C,d:D,e:E,f:F,g:G,h:H) = HCons(a,hList(b,c,d,e,f,g,h))
fun <A,B,C,D,E,F,G,H,I> hList(a:A,b:B,c:C,d:D,e:E,f:F,g:G,h:H,i:I) = HCons(a,hList(b,c,d,e,f,g,h,i))

data class HListPrepend<V>(val value:V) {
     fun <A:HList> invoke(hlist: A): HCons<V, A> =
      HCons(value,hlist)
}

class HListIdentity{
    fun <A:HList> invoke(hlist:A):A =
      hlist
}