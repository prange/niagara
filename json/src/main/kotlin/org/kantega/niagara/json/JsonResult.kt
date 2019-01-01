package org.kantega.niagara.json

import arrow.core.Either
import arrow.core.Tuple2
import arrow.core.Tuple3
import arrow.core.toOption
import arrow.data.NonEmptyList
import arrow.syntax.function.curried

data class JsonResult<out A>(val value: Either<NonEmptyList<String>, A>) {


    fun <T> fold(onFail: (NonEmptyList<String>) -> T, onSuccess: (A) -> T) =
      value.fold(onFail, onSuccess)

    fun <B> bind(f: (A) -> JsonResult<B>): JsonResult<B> =
      value.fold({ nel -> JsonResult.fail(nel) }, { a -> f(a) })

    fun <B> map(f: (A) -> B): JsonResult<B> =
      bind({ a ->
          try {
              jOk(f(a))
          } catch (e: Exception) {
              jFail(e.javaClass.simpleName + ":" + e.message)
          }
      })

    override fun toString(): String {
        return fold({ f -> "JsonResult(${f.all.joinToString { it }})" }, { s -> "JsonResult("+s.toString()+")" })
    }

    companion object {

        fun fail(t: Throwable) =
          JsonResult.fail(t.javaClass.simpleName + ":" + t.message.orEmpty())

        fun fail(msg: String) =
          JsonResult.fail(NonEmptyList.just(msg))

        fun fail(nel: NonEmptyList<String>) =
          JsonResult(Either.left(nel))

        fun <A> success(a: A): JsonResult<A> =
          JsonResult(Either.right(a))
    }
}

infix fun <A, B> JsonResult<(A) -> B>.apply(v: JsonResult<A>): JsonResult<B> =
  when {
      this.value is Either.Right && v.value is Either.Right -> jOk(this.value.b(v.value.b))
      this.value is Either.Left && v.value is Either.Left -> JsonResult.fail(this.value.a + v.value.a)
      this.value is Either.Left -> JsonResult.fail(this.value.a)
      v.value is Either.Left -> JsonResult.fail(v.value.a)
      else -> throw Error("unreachable code")
  }

fun <A> JsonResult<JsonValue>.decode(decoder: JsonDecoder<A>): JsonResult<A> =
  bind(decoder)

fun jFail(reason: String) =
  JsonResult.fail(reason)

fun <A> jOk(value: A) =
  JsonResult.success(value)

fun <A, B> jLift(f: (A) -> B) =
  jOk(f)

infix fun <A> JsonResult<A>.orElse(a: A): A =
  this.value.fold({ _ -> a }, { c -> c })

infix fun <A> JsonResult<A>.orElse(a: JsonResult<A>): JsonResult<A> =
  this.value.fold({ _ -> a }, { _ -> this })

infix fun <A> JsonResult<A>.orElse(f: () -> JsonResult<A>): JsonResult<A> =
  this.value.fold({ _ -> f() }, { _ -> this })


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