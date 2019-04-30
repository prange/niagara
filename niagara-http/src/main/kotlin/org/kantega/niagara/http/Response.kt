package org.kantega.niagara.http


import io.vavr.collection.List
import io.vavr.collection.TreeMap
import io.vavr.control.Option
import org.kantega.niagara.eff.Task
import org.kantega.niagara.data.update
import java.io.ByteArrayInputStream
import java.io.InputStream

data class Response(
  val responseCookies: TreeMap<String, Cookie>,
  val responseHeaders: TreeMap<String, List<String>>,
  val statusCode: Int,
  val body: Option<InputStream>) {

    fun withHeader(key: String, value: String): Response =
      copy(responseHeaders = responseHeaders.update(key, { list -> list.prepend(value) }, { List.of(value) }))


    fun withStatusCode(statusCode: Int): Response =
      copy(statusCode = statusCode)

    fun withBody(value: String): Response =
      withBody(ByteArrayInputStream(value.toByteArray(Charsets.UTF_8)))

    fun withBody(source: InputStream): Response =
      copy(body = Option.of(source))

    fun mediaType(mediaType: MediaType) {
        withHeader("Content-Type", mediaType.type)
    }

    fun mediaType(): MediaType =
      responseHeaders.get("Content-Type").flatMap { it.headOption() }.map { MediaType(it) }.getOrElse(MEDIA_WILDCARD)

    fun readBodyAsString(): Task<String> =
      body.fold(
        { Task.just("") },
        { Task.exec { it.reader().readText() } }
      )
}





