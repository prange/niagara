package org.kantega.niagara.http.example

import org.kantega.niagara.http.*
import io.vavr.kotlin.component1
import io.vavr.kotlin.component2
fun main() {

    val routes =
      (GET / "some" / "path" / "to" / pathParam / "tailed"){ req, param ->
          Ok(param)
      } or
        (GET / "some" / "other" / pathParam + queryParam("q")){ req, fromPath, fromQuery ->
            Ok(fromPath + fromQuery)
        }


    val result1 = routes.handle(Request.getRequest("/some/path/to/qwert/tailed"))
    val result2 = routes.handle(Request.getRequest("/some/other/jalla?q=ahfjksafb"))
    val result3 = routes.handle(Request.getRequest("/should/not/match"))


    println(result1.map { (req,resp) -> resp })
    println(result2.map { (req,resp) -> resp })
    println(result3.map { (req,resp) -> resp })
}