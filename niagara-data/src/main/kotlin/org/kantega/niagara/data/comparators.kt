package org.kantega.niagara.data

val compareString: Comparator<String> =
  Comparator.naturalOrder()

fun <A, B> Comparator<A>.comap(f: (B) -> A): Comparator<B> = Comparator { b1, b2 ->
    compare(f(b1), f(b2))
}
