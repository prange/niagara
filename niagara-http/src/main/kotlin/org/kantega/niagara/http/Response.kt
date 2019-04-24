package org.kantega.niagara.http


import io.vavr.collection.List
import io.vavr.collection.TreeMap
import io.vavr.control.Option
import org.kantega.niagara.data.update

data class Response(
  val responseCookies: TreeMap<String, Cookie>,
  val responseHeaders: TreeMap<String, List<String>>,
  val statusCode: Int,
  val body: Option<String>) {

    fun withHeader(key: String, value: String): Response =
      copy(responseHeaders = responseHeaders.update(key, { list -> list.prepend(value) }, { List.of(value) }))


    fun withStatusCode(statusCode: Int): Response =
      copy(statusCode = statusCode)

    fun withBody(value: String): Response =
      copy(body = Option.of(value))

}

fun Ok(): Response {
    return Response(TreeMap.empty(), TreeMap.empty(), 200, Option.none())
}

fun Ok(body: String): Response {
    return Ok().withBody(body)
}

fun body(value: String): Response {
    return Ok().withBody(value)
}