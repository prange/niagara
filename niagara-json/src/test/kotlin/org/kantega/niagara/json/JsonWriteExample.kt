package org.kantega.niagara.json

import org.kantega.niagara.json.io.JsonWriter

fun main() {
    val json =
    JsonObject(
      "name" to JsonString("Ola"),
      "age" to JsonNumber(43),
      "address" to JsonObject(
        "street" to JsonString("Northstreet"),
        "num" to JsonString("44"),
        "city" to JsonString("Oslo")
      )
    )

    val arr =
      JsonArray(JsonString("a"),JsonString("b"))

    println(JsonWriter.writePretty(json,4))
    println(JsonWriter.writePretty(arr,4))
}