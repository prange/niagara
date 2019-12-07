package org.kantega.niagara.json.examples

import org.kantega.niagara.json.JsonResult
import org.kantega.niagara.json.JsonValue
import org.kantega.niagara.json.io.JsonParser


fun parse():JsonResult<JsonValue> {
    val input = """
        { 
            "name":"Ola Normann"
            , "stereotype":"brogrammer"
            , "level":3
            , "traits":["tattoo","beard","bun"]
        }
    """.trimIndent()

   val json:JsonResult<JsonValue> =
     JsonParser.parse(input)

    println(json)

    return json
}



