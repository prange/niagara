package org.kantega.niagara.http

import io.vavr.Tuple2
import io.vavr.collection.List
import io.vavr.collection.TreeMap
import io.vavr.control.Option
import org.kantega.niagara.data.update
import java.io.UnsupportedEncodingException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class Request(
  val requestCookies: TreeMap<String, Cookie>,
  val requestHeaders: TreeMap<String, List<String>>,
  val queryParams: TreeMap<String, List<String>>,
  val body: String,
  val requestUri: URI,
  val remainingPath: List<String>,
  val resolvedPath: List<String>,
  val method: String) {

    fun withHeader(name: String, value: String) =
      copy(requestHeaders = requestHeaders.update(name, { list -> list.prepend(value) }, { List.of(value) }))


    fun withQueryParam(name: String, value: String) =
      copy(queryParams = queryParams.update(name, { list -> list.prepend(value) }, { List.of(value) }))


    fun withQueryParams(map: TreeMap<String, List<String>>): Request {
        val qp = queryParams
          .toList()
          .foldLeft(map,
            { tree, entry -> tree.update(entry._1(), { list -> list.prependAll(entry._2()) }, { entry._2() }) })
        return copy(queryParams = qp)
    }

    fun withBody(body: String) =
      copy(body = body)


    fun advancePath(count: Int): Request =
      if (count <= 0) this
      else copy(resolvedPath = resolvedPath.prepend(remainingPath.head()),
        remainingPath = remainingPath.tail()).advancePath(count - 1)


    companion object {

        fun getRequest(path: String): Request {
            val uri = URI.create(path)
            val unresoved = List.of<String>(*uri.path.substringBefore("?").split("/".toRegex()).filter({ it.isNotEmpty() }).toTypedArray())
            val params = params(uri)
            return Request(TreeMap.empty(), TreeMap.empty(), params, "", uri, unresoved, List.empty(), "GET")
        }

        fun post(path: String): Request {
            val uri = URI.create(path)
            val unresoved = List.of<String>(*uri.path.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
            return Request(TreeMap.empty(), TreeMap.empty(), TreeMap.empty(), "", uri, unresoved, List.empty(), "POST")
        }

        fun params(uri: URI): TreeMap<String, List<String>> {
            val headerString = Option.of(uri.rawQuery).getOrElse("")

            return List.of<String>(*headerString.split("&".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
              .map<Tuple2<String, String>> { keyValue ->
                  val split = keyValue.split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                  val key = split[0]
                  val value = decode(split[1])
                  Tuple2(key, value)
              }
              .foldLeft(TreeMap.empty(), { tree, tuple -> tree.update(tuple._1, { list -> list.prepend(tuple._2) }, { List.of(tuple._2) }) })
        }

        private fun decode(s: String): String {
            try {
                return URLDecoder.decode(s, StandardCharsets.UTF_8.toString())
            } catch (e: UnsupportedEncodingException) {
                throw RuntimeException(e)
            }

        }
    }
}
