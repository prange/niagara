package org.kantega.niagara.data


interface Product1<A>{
    fun component1():A
    fun hList():HList1<A> = hList(component1())
}

interface Product2<A,B>{
    fun component1():A
    fun component2():B
    fun hList():HList2<A,B> = hList(component1(),component2())
}

interface Product3<A,B,C>{
    fun component1():A
    fun component2():B
    fun component3():C
    fun hList():HList3<A,B,C> = hList(component1(),component2(),component3())
}