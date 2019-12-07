package org.kantega.niagara.json.examples

import org.kantega.niagara.json.*

fun readSafely() {
    val json: JsonResult<JsonValue> = parse()

    val nameResult = //Should be a JsonSuccess wrapping the name
      json.field("name").asString()

    //Get the value using a when expression
    val value =
      when (nameResult) {
          is JsonSuccess -> nameResult.value
          is JsonFail    -> nameResult.failures.toList().mkString(", ")
      }

    //Get the value using fold
    val foldValue =
      nameResult.fold(
        { failures -> failures.toList().mkString(", ") },
        { success -> success })
}