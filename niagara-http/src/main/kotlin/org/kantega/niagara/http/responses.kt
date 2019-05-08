package org.kantega.niagara.http

import io.vavr.collection.TreeMap
import io.vavr.control.Option
import org.kantega.niagara.data.appendIfMissing
import java.io.InputStream

val Ok =
  Response(TreeMap.empty(), TreeMap.empty(), 200, Option.none())

val NotFound =
  Ok.withStatusCode(404)

fun Ok(body: String): Response {
    return Ok.withBody(body)
}

fun Ok(value: InputStream): Response {
    return Ok.withBody(value)
}


fun classPathResources(prefix: String): (Request) -> Response = { request ->
    val path =
      prefix.appendIfMissing("/")+request.remainingPath.mkString("/")

    val maybeInputStream =
      Option.of(Thread.currentThread().contextClassLoader.getResourceAsStream(path))

    maybeInputStream.fold(
      { NotFound },
      { inputStream -> Ok(inputStream) }
    )

}

fun classPathResource(name: String): (Request) -> Response = { request ->


    val maybeInputStream =
      Option.of(Thread.currentThread().contextClassLoader.getResourceAsStream(name))

    maybeInputStream.fold(
      { NotFound },
      { inputStream -> Ok(inputStream) }
    )

}