package org.kantega.niagara.http


import io.undertow.server.handlers.Cookie
import io.undertow.util.HeaderMap
import io.undertow.util.HttpString
import io.vavr.collection.List
import io.vavr.collection.TreeMap
import org.kantega.niagara.data.update

data class Response(
  val responseCookies: TreeMap<String, Cookie>,
  val responseHeaders: TreeMap<HttpString, List<String>>,
  val statusCode: Int,
  val body: String) {

    fun withHeader(key: String, value: String): Response =
      copy(responseHeaders = responseHeaders.update(HttpString(key), { list -> list.prepend(value) }, { List.of(value) }))


    fun withStatusCode(statusCode: Int): Response =
      copy(statusCode = statusCode)

    fun withBody(value: String): Response =
      copy(body = value)


    companion object {
/*
        fun fromExchange(exchange: HttpServerExchange): Response =
          Response(TreeMap.ofAll(exchange.responseCookies), exchange.responseHeaders, exchange.statusCode, "")
*/

    }
}

fun Ok(): Response {
    return Response(TreeMap.empty(), TreeMap.empty(), 200, "")
}

fun Ok(body: String): Response {
    return Ok().withBody(body)
}

fun body(value: String): Response {
    return Ok().withBody(value)
}