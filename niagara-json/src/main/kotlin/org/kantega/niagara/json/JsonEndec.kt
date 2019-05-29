package org.kantega.niagara.json

import org.kantega.niagara.data.*


data class JsonEndec<A>(val encoder:JsonEncoder<A>, val decoder: JsonDecoder<A>){

    fun <B> xmap(f:(A)->B,g:(B)->A):JsonEndec<B> = JsonEndec(
      encoder.comap(g),
      decoder.map(f)
    )

}

data class EndecBuilder<OBJ,C,REST>(val encoder:JsonObjectBuilder<OBJ,REST>, val decoder: JsonDecoder<C>)

fun <OBJ,FIRST,REST,TAIL:HList> EndecBuilder<OBJ,(FIRST)->REST,HCons<FIRST,TAIL>>
  .field(name:String,endec:JsonEndec<FIRST>):EndecBuilder<OBJ,REST,TAIL> =
  EndecBuilder(encoder.field(name,endec.encoder),decoder.field(name,endec.decoder))

fun <A> endec( encoder:JsonEncoder<A>,  decoder: JsonDecoder<A>) : JsonEndec<A> =
  JsonEndec(encoder,decoder)


val endecString = JsonEndec(encodeString, decodeString)
val endecInt = JsonEndec(encodeInt, decodeInt)
val endecLong = JsonEndec(encodeLong, decodeLong)
val endecDouble = JsonEndec(encodeDouble, decodeDouble)
val endecBool = JsonEndec(encodeBool, decodeBool)
fun <A> endecArray(elemEndec:JsonEndec<A>) = JsonEndec(encodeArray(elemEndec.encoder), decodeArray(elemEndec.decoder))


fun <A, B, C, D, E, F, G, H, I, J,T> endec(constructor: (A, B, C, D, E, F, G, H, I,J) -> T,destructor:(T)->HList10<A,B,C,D,E,F,G,H,I,J>) =
  EndecBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B, C, D, E, F, G, H, I, T> endec(constructor: (A, B, C, D, E, F, G, H, I) -> T,destructor:(T)->HList9<A,B,C,D,E,F,G,H,I>) =
  EndecBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B, C, D, E, F, G, H, T> endec(constructor: (A, B, C, D, E, F, G, H) -> T,destructor:(T)->HList8<A,B,C,D,E,F,G,H>) =
  EndecBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B, C, D, E, F, G, T> endec(constructor: (A, B, C, D, E, F, G) -> T,destructor:(T)->HList7<A,B,C,D,E,F,G>) =
  EndecBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B, C, D, E, F, T> endec(constructor: (A, B, C, D, E, F) -> T,destructor:(T)->HList6<A,B,C,D,E,F>) =
  EndecBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B, C, D, E,T> endec(constructor: (A, B, C, D, E) -> T,destructor:(T)->HList5<A,B,C,D,E>) =
  EndecBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B, C, D, T> endec(constructor: (A, B, C, D) -> T,destructor:(T)->HList4<A,B,C,D>) =
  EndecBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B, C, T> endec(constructor: (A, B, C) -> T,destructor:(T)->HList3<A,B,C>) =
  EndecBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B,T> endec(constructor: (A, B) -> T,destructor:(T)->HList2<A,B>) =
  EndecBuilder(encode(destructor),decode(constructor.curried()))

fun <A, T> endec(constructor: (A) -> T,destructor:(T)->HList1<A>) =
  EndecBuilder(encode(destructor),decode(constructor))

