package org.kantega.niagara.json

import io.vavr.collection.List
import org.kantega.niagara.data.NonEmptyList
import org.kantega.niagara.data.Semigroup


sealed class JsonResult<out A> {


    abstract fun <T> fold(onFail: (NonEmptyList<String>) -> T, onSuccess: (A) -> T): T

    inline fun <reified B> bind(crossinline f: (A) -> JsonResult<B>): JsonResult<B> =
      fold(
        { nel -> fail(nel) },
        { a ->
            try {
                f(a)
            } catch (e: Exception) {
                val tpe = B::class.java.simpleName
                jFail("Could not convert $a to JsonResult<$tpe>: ${e.javaClass.simpleName} : ${e.message}")
            }
        })

    inline fun <reified B> map(crossinline f: (A) -> B): JsonResult<B> =
      bind({ a ->
          try {
              jOk(f(a))
          } catch (e: Exception) {
              val tpe = B::class.java.simpleName
              jFail<B>("Could not convert $a to $tpe: ${e.javaClass.simpleName} : ${e.message}")
          }
      })

    override fun toString(): String {
        return fold({ f -> "JsonResult(${f.toList().joinToString { it }})" }, { s -> "JsonResult(" + s.toString() + ")" })
    }


    companion object {

        fun <A> fail(t: Throwable): JsonResult<A> =
          fail(t.javaClass.simpleName + ":" + t.message.orEmpty())

        fun <A> fail(msg: String, vararg tail: String): JsonResult<A> =
          fail(NonEmptyList.of(msg, *tail))

        fun <A> fail(nel: NonEmptyList<String>): JsonResult<A> =
          JsonFail(nel)

        fun <A> success(a: A): JsonResult<A> =
          JsonSuccess(a)
    }
}

data class JsonSuccess<A>(val value: A) : JsonResult<A>() {
    override fun <T> fold(onFail: (NonEmptyList<String>) -> T, onSuccess: (A) -> T) =
      onSuccess(value)

}

data class JsonFail<A>(val failures: NonEmptyList<String>) : JsonResult<A>() {
    override fun <T> fold(onFail: (NonEmptyList<String>) -> T, onSuccess: (A) -> T): T =
      onFail(failures)

}

infix fun <A, B> JsonResult<(A) -> B>.apply(v: JsonResult<A>): JsonResult<B> =
  when {
      this is JsonSuccess && v is JsonSuccess -> jOk(this.value(v.value))
      this is JsonFail && v is JsonFail       -> JsonResult.fail(this.failures + v.failures)
      this is JsonFail                        -> JsonResult.fail(this.failures)
      v is JsonFail                           -> JsonResult.fail(v.failures)
      else                                    -> throw Error("unreachable code")
  }

inline fun <reified A> JsonResult<JsonValue>.decode(crossinline decoder: JsonDecoder<A>): JsonResult<A> =
  bind(decoder)

fun <A> jFail(reason: String) =
  JsonResult.fail<A>(reason)

fun <A> jOk(value: A) =
  JsonResult.success(value)

fun <A, B> jLift(f: (A) -> B) =
  jOk(f)

infix fun <A> JsonResult<A>.getOrElse(a: A): A =
  this.fold({ a }, { it })

infix fun <A> JsonResult<A>.getOrElse(f: (NonEmptyList<String>) -> A): A =
  this.fold({ f(it) }, { it })

infix fun <A> JsonResult<A>.getOrElseThrow(f: (NonEmptyList<String>) -> Throwable): A =
  this.fold({ nel -> throw f(nel) }, { it })

infix fun <A> JsonResult<A>.orElse(a: JsonResult<A>): JsonResult<A> =
  this.fold({ a }, { this })

infix fun <A> JsonResult<A>.orElse(f: (NonEmptyList<String>) -> JsonResult<A>): JsonResult<A> =
  this.fold({ nel -> f(nel) }, { this })

fun JsonResult<JsonValue>.field(path: String): JsonResult<JsonValue> =
  this.field(JsonPath(path))

fun JsonResult<JsonValue>.field(path: JsonPath): JsonResult<JsonValue> =
  this.bind { value -> path.get(value) }

fun JsonResult<JsonValue>.asArray(): JsonResult<JsonArray> =
  this.bind { v -> v.asArray() }

fun JsonResult<JsonValue>.asObject(): JsonResult<JsonObject> =
  this.bind { v -> v.asObject() }

fun JsonResult<JsonArray>.mapJsonArray(f: (JsonValue) -> JsonResult<JsonValue>): JsonResult<JsonArray> =
  this.bind { array ->
      array.values.map(f).sequence().map { JsonArray(it) }
  }

fun <A> List<JsonResult<A>>.sequence(): JsonResult<List<A>> =
  this.foldLeft(
    JsonResult.success(List.empty<A>()),
    { accum, elem ->
        accum.fold(
          { accumFail ->
              elem.fold(
                { fail -> JsonResult.fail(accumFail + fail) },
                { JsonResult.fail(accumFail) })
          },
          { list ->
              elem.fold(
                { fail -> JsonResult.fail(fail) },
                { value -> JsonResult.success(list.append(value)) })
          })
    }
  )

fun JsonResult<JsonValue>.asString(): JsonResult<String> =
  this.bind { it.asString() }

fun JsonResult<JsonValue>.asInt(): JsonResult<Int> =
  this.bind { it.asNumber() }.map { bd -> bd.toInt() }

inline fun <A, B, reified C> bind(aResult: JsonResult<A>, bResult: JsonResult<B>, crossinline joinF: (A, B) -> C): JsonResult<C> =
  aResult.bind { a -> bResult.map { b -> joinF(a, b) } }

inline fun <A, B, C, reified D> bind(aResult: JsonResult<A>, bResult: JsonResult<B>, cResult: JsonResult<C>, crossinline joinF: (A, B, C) -> D): JsonResult<D> =
  aResult.bind { a ->
      bResult.bind { b -> cResult.map { c -> joinF(a, b, c) } }
  }

data class JsonResultSemigroup<A>(val aSemigroup: Semigroup<A>) : Semigroup<JsonResult<A>> {
    override fun invoke(p1: JsonResult<A>, p2: JsonResult<A>): JsonResult<A> {
        return when {
            p1 is JsonSuccess && p2 is JsonSuccess -> jOk(aSemigroup(p1.value, p2.value))
            p1 is JsonFail && p2 is JsonFail       -> JsonResult.fail(p1.failures + p2.failures)
            p1 is JsonFail && p2 is JsonSuccess    -> JsonResult.fail(p1.failures)
            p1 is JsonSuccess && p2 is JsonFail    -> JsonResult.fail(p2.failures)
            else                                   -> throw Error("unreachable code")
        }
    }
}
