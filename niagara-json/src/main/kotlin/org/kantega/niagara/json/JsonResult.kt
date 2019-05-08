package org.kantega.niagara.json

import org.kantega.niagara.data.NonEmptyList


sealed class JsonResult<out A> {


    abstract fun <T> fold(onFail: (NonEmptyList<String>) -> T, onSuccess: (A) -> T): T

    fun <B> bind(f: (A) -> JsonResult<B>): JsonResult<B> =
      fold({ nel -> JsonResult.fail(nel) }, { a -> f(a) })

    fun <B> map(f: (A) -> B): JsonResult<B> =
      bind({ a ->
          try {
              jOk(f(a))
          } catch (e: Exception) {
              jFail<B>(e.javaClass.simpleName + ":" + e.message)
          }
      })

    override fun toString(): String {
        return fold({ f -> "JsonResult(${f.toList().joinToString { it }})" }, { s -> "JsonResult(" + s.toString() + ")" })
    }

    companion object {

        fun <A> fail(t: Throwable): JsonResult<A> =
          JsonResult.fail(t.javaClass.simpleName + ":" + t.message.orEmpty())

        fun <A> fail(msg: String, vararg tail: String): JsonResult<A> =
          JsonResult.fail(NonEmptyList.of(msg, *tail))

        fun <A> fail(nel: NonEmptyList<String>) =
          JsonFail<A>(nel)

        fun <A> success(a: A): JsonResult<A> =
          JsonSuccess(a)
    }
}

data class JsonSuccess<A>(val a: A) : JsonResult<A>() {
    override fun <T> fold(onFail: (NonEmptyList<String>) -> T, onSuccess: (A) -> T) =
      onSuccess(a)

}

data class JsonFail<A>(val failNel: NonEmptyList<String>) : JsonResult<A>() {
    override fun <T> fold(onFail: (NonEmptyList<String>) -> T, onSuccess: (A) -> T): T =
      onFail(failNel)

}

infix fun <A, B> JsonResult<(A) -> B>.apply(v: JsonResult<A>): JsonResult<B> =
  when {
      this is JsonSuccess && v is JsonSuccess -> jOk(this.a(v.a))
      this is JsonFail && v is JsonFail       -> JsonResult.fail(this.failNel + v.failNel)
      this is JsonFail                        -> JsonResult.fail(this.failNel)
      v is JsonFail                           -> JsonResult.fail(v.failNel)
      else                                    -> throw Error("unreachable code")
  }

fun <A> JsonResult<JsonValue>.decode(decoder: JsonDecoder<A>): JsonResult<A> =
  bind(decoder)

fun <A> jFail(reason: String) =
  JsonResult.fail<A>(reason)

fun <A> jOk(value: A) =
  JsonResult.success(value)

fun <A, B> jLift(f: (A) -> B) =
  jOk(f)

infix fun <A> JsonResult<A>.getOrElse(a: A): A =
  this.fold({ a }, { c -> c })

infix fun <A> JsonResult<A>.getOrElse(f: (NonEmptyList<String>) -> A): A =
  this.fold({ f(it) }, { c -> c })

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

fun JsonResult<JsonValue>.asString(): JsonResult<String> =
  this.bind { it.asString() }

fun JsonResult<JsonValue>.asInt(): JsonResult<Int> =
  this.bind { it.asNumber() }.map { bd -> bd.toInt() }