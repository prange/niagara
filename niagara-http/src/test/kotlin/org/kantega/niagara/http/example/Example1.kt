package org.kantega.niagara.http.example

import org.kantega.niagara.http.*
import io.vavr.kotlin.component1
import io.vavr.kotlin.component2

fun main() {

    val routes =
      routes(
        (GET / "some" / "path" / "to" / pathParam / "tailed"){ _, param ->
            Ok(param)
        },
        (GET / "some" / "other" / pathParam / queryParam("q")){ _, fromPath, fromQuery ->
            Ok(fromPath + fromQuery)
        }
      )

    val result1 = routes.handle(Request.getRequest("/some/path/to/qwert/tailed"))
    val result2 = routes.handle(Request.getRequest("/some/other/jalla?q=ahfjksafb"))
    val result3 = routes.handle(Request.getRequest("/should/not/match"))


    println(result1.map { (_, resp) -> resp })
    println(result2.map { (_, resp) -> resp })
    println(result3.map { (_, resp) -> resp })
}