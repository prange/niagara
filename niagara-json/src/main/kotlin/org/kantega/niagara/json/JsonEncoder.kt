package org.kantega.niagara.json

import io.vavr.collection.List
import io.vavr.kotlin.*
import org.kantega.niagara.data.*
import java.math.BigDecimal

typealias JsonEncoder<A> = (A) -> JsonValue

fun <A, B> JsonEncoder<B>.comap(f: (A) -> B): JsonEncoder<A> = { a ->
    invoke(f(a))
}


data class JsonObjectBuilder<A, B>(val f: (JsonObject, A) -> P2<JsonObject, B>) : JsonEncoder<A> {
    override fun invoke(a: A): JsonValue =
      f(JsonObject(), a)._1

    fun <C> append(f: (JsonObject, B) -> P2<JsonObject, C>): JsonObjectBuilder<A, C> = JsonObjectBuilder { jsonObject, a ->
        val (nextObj, b) = f(jsonObject, a)
        f(nextObj, b)
    }
}


fun <A, B, T : HList> encode(destructor: (A) -> HCons<B, T>): JsonObjectBuilder<A, HCons<B, T>> =
  JsonObjectBuilder({ jsonObj, a -> p(jsonObj, destructor(a)) })

fun <A, B, T, L : HCons<B, T>> JsonObjectBuilder<A, L>.field(name: String, aEncoder: JsonEncoder<B>): JsonObjectBuilder<A, T> =
  append { jsonObj, hList ->
      p(jsonObj.set(name, aEncoder(hList.head)), hList.tail)
  }

fun <A, L : HList> JsonObjectBuilder<A, L>.value(name: String, value: JsonValue): JsonObjectBuilder<A, L> =
  append { jsonObj, hList ->
      p(jsonObj.set(name, value), hList)
  }


val encodeString: JsonEncoder<String> = ::JsonString

val encodeNumber: JsonEncoder<BigDecimal> = ::JsonNumber

val encodeInt: JsonEncoder<Int> = { n -> JsonNumber(n.toBigDecimal()) }

val encodeLong: JsonEncoder<Long> = { n -> JsonNumber(n.toBigDecimal()) }

val encodeDouble: JsonEncoder<Double> = { n -> JsonNumber(n.toBigDecimal()) }

val encodeBool: JsonEncoder<Boolean> = ::JsonBool

fun <A> encodeArray(elemEncoder: JsonEncoder<A>): JsonEncoder<List<A>> = { list ->
    JsonArray(list.map { a -> elemEncoder(a) })
}


data class User(val name: String, val age: Int) : Product2<String, Int>