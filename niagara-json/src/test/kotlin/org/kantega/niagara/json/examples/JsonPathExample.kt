package org.kantega.niagara.json.examples

import org.kantega.niagara.json.*
import org.kantega.niagara.json.io.JsonWriter

fun main(args: Array<String>) {


    val data =
      Customer(
        "Test",
        Location("Trondheim", "Norway"),
        ContactInfo("12345678", "test@test.com"))

    val json =
      customerToJson(data)


    println(JsonWriter.writePretty(json))

    val pathToPhone =
      JsonPath("info.phone")

    val jsonWithPhoneUpdated =
      pathToPhone.updateResult(json, { js -> js.asString().map { s -> JsonString(s.reversed()) } })

    println(JsonWriter.writePretty(jsonWithPhoneUpdated getOrElse JsonString("")))

}

fun customerToJson(c: Customer): JsonValue =
  JsonObject(
    "name" to JsonString(c.name),
    "location" to JsonObject(
      "city" to JsonString(c.location.city),
      "country" to JsonString(c.location.country)
    ),
    "info" to JsonObject(
      "phone" to JsonString(c.info.phone),
      "email" to JsonString(c.info.email)
    )
  )

data class Location(val city: String, val country: String)
data class ContactInfo(val phone: String, val email: String)
data class Customer(val name: String, val location: Location, val info: ContactInfo)