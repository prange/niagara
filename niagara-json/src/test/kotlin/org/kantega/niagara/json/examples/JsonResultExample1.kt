package org.kantega.niagara.json.examples

import org.kantega.niagara.json.*

fun readFields() {
    //From the first example
    val json: JsonResult<JsonValue> = parse()

    val nameResult = //Should be a JsonSuccess wrapping the name
      json.field("name").asString()


    val failedTraits = //This is a failure, since the field "traits" is not a string but an array
      json.field("traits").asString()


    val notExistsField =  //This is also a failure, no field named "age" is present
      json.field("age").asInt()
}