package org.kantega.niagara.http.util

import java.util.function.Function

object StringUtils {


    fun <A> upTo(string: String, at: String, f: Function<String, A>): A {
        val index = string.indexOf(at)
        return if (index == -1) f.apply(string) else f.apply(string.substring(0, index))

    }
}
