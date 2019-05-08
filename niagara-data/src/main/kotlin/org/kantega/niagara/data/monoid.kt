package org.kantega.niagara.data

interface Monoid<A>{

    fun zero():A

    fun append(one:A,other:A):A

    fun mappend(one:A,other:A) =
      append(one,other)

}