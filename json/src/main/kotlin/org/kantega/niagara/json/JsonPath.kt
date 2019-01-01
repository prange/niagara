package org.kantega.niagara.json

import io.vavr.collection.List

interface JsonPath {

    fun set(target: JsonValue, value: JsonValue): JsonResult<JsonValue>
    fun get(target: JsonValue): JsonResult<JsonValue>
    fun update(target: JsonValue, f: (JsonValue) -> JsonValue): JsonResult<JsonValue> =
      get(target).map(f).bind { set(target, it) }

    fun updateResult(target: JsonValue, f: (JsonValue) -> JsonResult<JsonValue>): JsonResult<JsonValue> =
      get(target).bind(f).bind { set(target, it) }

    companion object{
        val self:JsonPath = SelfPath

        operator fun invoke(str:String) =
          List.ofAll(str.split('.'))
            .foldRight(self,{part,accum->FieldPath(part,accum)})
    }
}

object SelfPath : JsonPath {
    override fun set(target: JsonValue, value: JsonValue): JsonResult<JsonValue> =
      jOk(value)

    override fun get(target: JsonValue): JsonResult<JsonValue> =
      jOk(target)

}

data class FieldPath(val name: String, val down: JsonPath) : JsonPath {
    override fun set(target: JsonValue, value: JsonValue): JsonResult<JsonValue> =
      target.asObject().bind { obj ->
          down
            .set(obj.m.get(name).getOrElse(JsonObject()), value)
            .map { updatedChild -> obj.set(name, updatedChild) }
      }


    override fun get(target: JsonValue): JsonResult<JsonValue> =
      target.field(name).bind(down::get)

}