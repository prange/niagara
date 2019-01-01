package org.kantega.niagara.typeclasses

data class Equal<A>(val f: (A, A) -> Boolean) : (A, A) -> Boolean by f {

    fun eq(pair: Pair<A, A>): Boolean =
            f(pair.first, pair.second)

    companion object {
        val stringEq = Equal<String> { a, b -> a == b }
    }
}

