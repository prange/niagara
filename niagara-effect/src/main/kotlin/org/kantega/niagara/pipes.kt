package org.kantega.niagara

typealias Pipe<A,B> = (Source<A>) -> Source<B>


fun <A,B> map(f:(A)->B):Pipe<A,B> = {
    it.map(f)
}
