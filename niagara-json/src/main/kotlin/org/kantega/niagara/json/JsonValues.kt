package org.kantega.niagara.json

import io.vavr.collection.List
import io.vavr.collection.TreeMap
import java.math.BigDecimal

sealed class JsonValue {

    fun <T> fold(
      onNull: (JsonNull) -> T,
      onBool: (JsonBool) -> T,
      onNumber: (JsonNumber) -> T,
      onString: (JsonString) -> T,
      onObject: (JsonObject) -> T,
      onArray: (JsonArray) -> T
    ): T {
        return when (this) {
            is JsonNull -> onNull(JsonNull)
            is JsonBool -> onBool(this)
            is JsonNumber -> onNumber(this)
            is JsonString -> onString(this)
            is JsonObject -> onObject(this)
            is JsonArray -> onArray(this)
        }
    }


    fun asNumber(): JsonResult<BigDecimal> =
      when(this){
          is JsonNumber -> JsonResult.success(n)
          else -> JsonResult.fail("You are trying to convert a $this to a number.")
      }

    fun asString(): JsonResult<String> =
      when(this){
          is JsonString -> JsonResult.success(s)
          else -> JsonResult.fail("You are trying to convert a $this to a string.")
      }

    fun asBoolean(): JsonResult<Boolean> =
      when(this){
          is JsonBool -> JsonResult.success(value)
          else -> JsonResult.fail("You are trying to convert a $this to a boolean.")
      }

    fun asArray(): JsonResult<JsonArray> =
      when(this){
          is JsonArray -> JsonResult.success(this)
          else -> JsonResult.fail("You are trying to convert a $this to an array.")
      }

    fun asObject(): JsonResult<JsonObject> =
      when(this){
          is JsonObject -> JsonResult.success(this)
          else -> JsonResult.fail("You are trying to convert a $this to an object.")
      }

    fun field(name: String): JsonResult<JsonValue> =
      asObject().bind { obj ->
          obj.m.get(name)
            .map { JsonResult.success(it) }
            .getOrElse(JsonResult.fail("The field $name did not exist in the object. Available fields are {${obj.m.keySet().mkString(", ")}}"))
      }

}

object JsonNull : JsonValue()

data class JsonBool(val value:Boolean) : JsonValue ()

data class JsonNumber(val n: BigDecimal) : JsonValue() {


    companion object {
        operator fun invoke(l: Long) =
          JsonNumber(BigDecimal.valueOf(l))

        operator fun invoke(l: Int) =
          JsonNumber(BigDecimal.valueOf(l.toLong()))
    }
}

data class JsonString(val s: String) : JsonValue() {

    override fun toString(): String {
        return "\"$s\""
    }
}



data class JsonObject(val m: TreeMap<String,JsonValue>) : JsonValue() {

    fun update(f: (TreeMap<String,JsonValue>) -> TreeMap<String,JsonValue>): JsonObject =
      copy(m = f(m))

    fun set(name: String, value: JsonValue) =
      JsonObject(m.put(name, value))

    companion object {
        operator fun invoke(vararg members:Pair<String,JsonValue>):JsonObject =
          JsonObject(
            members.fold(
              TreeMap.empty(),
              {map,member->map.put(member.first,member.second)}))

    }


    override fun toString(): String {
        return m.toList().joinToString (
          prefix = "JsonObject(",
          postfix = ")",
          separator = ", ",
          limit = 40){ t->t._1()+":"+t._2() }
    }
}

data class JsonArray(val a: List<JsonValue>) : JsonValue() {

    fun update(f:(List<JsonValue>)->List<JsonValue>) =
      copy(a = f(a))



    companion object {
        operator fun invoke(vararg elements:JsonValue) =
          JsonArray(List.ofAll(elements.asIterable()))
    }
}