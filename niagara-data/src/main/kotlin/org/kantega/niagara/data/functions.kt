package org.kantega.niagara.data

infix fun <A,B,C> ((A)->B).andThen(g:(B)->C):(A)->C = {a->
    g(this(a))
}
