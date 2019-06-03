package org.kantega.niagara.data


interface Product1<A>{
    fun component1():A
    fun hList() = hList(component1())
}

interface Product2<A,B>{
    fun component1():A
    fun component2():B
    fun hList() = hList(component1(),component2())
}

interface Product3<A,B,C>{
    fun component1():A
    fun component2():B
    fun component3():C
    fun hList() = hList(component1(),component2(),component3())
}

interface Product4<A,B,C,D>{
    fun component1():A
    fun component2():B
    fun component3():C
    fun component4():D
    fun hList() = hList(component1(),component2(),component3(),component4())
}

interface Product5<A,B,C,D,E>{
    fun component1():A
    fun component2():B
    fun component3():C
    fun component4():D
    fun component5():E
    fun hList() = hList(component1(),component2(),component3(),component4(),component5())
}

interface Product6<A,B,C,D,E,F>{
    fun component1():A
    fun component2():B
    fun component3():C
    fun component4():D
    fun component5():E
    fun component6():F
    fun hList() = hList(component1(),component2(),component3(),component4(),component5(),component6())
}

interface Product7<A,B,C,D,E,F,G>{
    fun component1():A
    fun component2():B
    fun component3():C
    fun component4():D
    fun component5():E
    fun component6():F
    fun component7():G
    fun hList() = hList(component1(),component2(),component3(),component4(),component5(),component6(),component7())
}

interface Product8<A,B,C,D,E,F,G,H>{
    fun component1():A
    fun component2():B
    fun component3():C
    fun component4():D
    fun component5():E
    fun component6():F
    fun component7():G
    fun component8():H
    fun hList() = hList(component1(),component2(),component3(),component4(),component5(),component6(),component7(),component8())
}

interface Product9<A,B,C,D,E,F,G,H,I>{
    fun component1():A
    fun component2():B
    fun component3():C
    fun component4():D
    fun component5():E
    fun component6():F
    fun component7():G
    fun component8():H
    fun component9():I
    fun hList() = hList(component1(),component2(),component3(),component4(),component5(),component6(),component7(),component8(),component9())
}