package org.kantega.niagara.json

import org.kantega.niagara.data.*


data class JsonMapper<A>(val encoder:JsonEncoder<A>, val decoder: JsonDecoder<A>){

    inline fun <reified B> xmap(crossinline f:(A)->B, noinline g:(B)->A):JsonMapper<B> = JsonMapper(
      encoder.comap(g),
      decoder.map(f)
    )

}

data class ObjectMapperBuilder<OBJ,C,REST>(val encoder:JsonObjectBuilder<OBJ,REST>, val decoder: JsonDecoder<C>)

fun <A> ObjectMapperBuilder<A,A,*>.build() =
  JsonMapper(this.encoder,this.decoder)

inline fun <OBJ,reified FIRST,reified REST,TAIL:HList> ObjectMapperBuilder<OBJ,(FIRST)->REST,HCons<FIRST,TAIL>>
  .field(name:String,endec:JsonMapper<FIRST>):ObjectMapperBuilder<OBJ,REST,TAIL> =
  ObjectMapperBuilder(encoder.field(name,endec.encoder),decoder.field(name,endec.decoder))

fun <A> mapper(encoder:JsonEncoder<A>, decoder: JsonDecoder<A>) : JsonMapper<A> =
  JsonMapper(encoder,decoder)


val mapString = JsonMapper(encodeString, decodeString)
val mapInt = JsonMapper(encodeInt, decodeInt)
val mapLong = JsonMapper(encodeLong, decodeLong)
val mapDouble = JsonMapper(encodeDouble, decodeDouble)
val mapBool = JsonMapper(encodeBool, decodeBool)
fun <A> mapArray(elemEndec:JsonMapper<A>) = JsonMapper(encodeArray(elemEndec.encoder), decodeArray(elemEndec.decoder))


fun <A, B, C, D, E, F, G, H, I, J,T> mapObject(constructor: (A, B, C, D, E, F, G, H, I, J) -> T, destructor:(T)->HList10<A,B,C,D,E,F,G,H,I,J>) =
  ObjectMapperBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B, C, D, E, F, G, H, I, T> mapObject(constructor: (A, B, C, D, E, F, G, H, I) -> T, destructor:(T)->HList9<A,B,C,D,E,F,G,H,I>) =
  ObjectMapperBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B, C, D, E, F, G, H, T> mapObject(constructor: (A, B, C, D, E, F, G, H) -> T, destructor:(T)->HList8<A,B,C,D,E,F,G,H>) =
  ObjectMapperBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B, C, D, E, F, G, T> mapObject(constructor: (A, B, C, D, E, F, G) -> T, destructor:(T)->HList7<A,B,C,D,E,F,G>) =
  ObjectMapperBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B, C, D, E, F, T> mapObject(constructor: (A, B, C, D, E, F) -> T, destructor:(T)->HList6<A,B,C,D,E,F>) =
  ObjectMapperBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B, C, D, E,T> mapObject(constructor: (A, B, C, D, E) -> T, destructor:(T)->HList5<A,B,C,D,E>) =
  ObjectMapperBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B, C, D, T> mapObject(constructor: (A, B, C, D) -> T, destructor:(T)->HList4<A,B,C,D>) =
  ObjectMapperBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B, C, T> mapObject(constructor: (A, B, C) -> T, destructor:(T)->HList3<A,B,C>) =
  ObjectMapperBuilder(encode(destructor),decode(constructor.curried()))

fun <A, B,T> mapObject(constructor: (A, B) -> T, destructor:(T)->HList2<A,B>) =
  ObjectMapperBuilder(encode(destructor),decode(constructor.curried()))

fun <A, T> mapObject(constructor: (A) -> T, destructor:(T)->HList1<A>) =
  ObjectMapperBuilder(encode(destructor),decode(constructor))

