package org.kantega.niagara.http

import arrow.core.Option
import arrow.core.Tuple2
import fj.F
import fj.Ord
import fj.P
import fj.data.List
import fj.data.NonEmptyList
import fj.data.TreeMap
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.Cookie
import io.undertow.util.HeaderMap
import io.undertow.util.HttpString

import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*

class Input private constructor(val requestCookies: TreeMap<String, Cookie>, val requestHeaders: HeaderMap, val queryParams: TreeMap<String, List<String>>, val body: String, val requestUri: URI, val remainingPath: List<String>, val resolvedPath: List<String>, val method: String, val maybeServerConnection: Option<HttpServerExchange>) {

    fun withHeader(name: String, value: String): Input {
        return Input(requestCookies, requestHeaders.add(HttpString(name), value), queryParams, body, requestUri, remainingPath, resolvedPath, method, maybeServerConnection)
    }

    fun withQueryParam(name: String, value: String): Input {
        return Input(requestCookies, requestHeaders, queryParams.update(name, { list -> list.cons(value) }, List.single(value)), body, requestUri, remainingPath, resolvedPath, method, maybeServerConnection)
    }

    fun withQueryParams(map: TreeMap<String, List<String>>): Input {
        val qp = queryParams
          .toList()
          .foldLeft(
            { tree, entry -> tree.update(entry._1(), { list -> list.append(entry._2()) }, entry._2()) }, map)
        return Input(requestCookies, requestHeaders, qp, body, requestUri, remainingPath, resolvedPath, method, maybeServerConnection)
    }

    fun withBody(body: String): Input {
        return Input(requestCookies, requestHeaders, queryParams, body, requestUri, remainingPath, resolvedPath, method, maybeServerConnection)
    }

    fun advancePath(count: Int): Input {
        return if (count <= 0) this else Input(requestCookies, requestHeaders, queryParams, body, requestUri, remainingPath.tail(), resolvedPath.cons(remainingPath.head()), method, maybeServerConnection)
          .advancePath(count - 1)
    }

    companion object {

        //Mjst be wrapped in a blocking handler that handles dispatch to a thread pool
        //Must also be able to dispatch TAsks to the same pool
        fun of(exchange: HttpServerExchange): Input =
             Input(
              TreeMap.fromMutableMap(Ord.stringOrd, exchange.requestCookies),
              exchange.requestHeaders,
              TreeMap.fromMutableMap<String, Deque<String>>(Ord.stringOrd, exchange.queryParameters).map(F{ List.iterableList(it) }),
              readBytes(exchange.inputStream),
              URI.create(exchange.requestURI),
              List.arrayList(*exchange.requestPath.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()),
              List.nil(),
              exchange.requestMethod.toString(),
              Option.just(exchange))



        private fun readBytes(stream: InputStream): String =
          stream.bufferedReader().use { it.readText() }


        operator fun get(path: String): Input {
            val uri = URI.create(path)
            val unresoved = List.arrayList<String>(*uri.path.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
            return Input(TreeMap.empty(Ord.stringOrd), HeaderMap(), TreeMap.empty(Ord.stringOrd), "", uri, unresoved, List.nil(), "GET", Option.empty())
        }

        fun post(path: String): Input {
            val uri = URI.create(path)
            val unresoved = List.arrayList<String>(*uri.path.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
            return Input(TreeMap.empty(Ord.stringOrd), HeaderMap(), TreeMap.empty(Ord.stringOrd), "", uri, unresoved, List.nil(), "POST", Option.empty())
        }

        fun headers(uri: URI): TreeMap<String, List<String>> {
            val headerString = uri.rawQuery

            return List.arrayList<String>(*headerString.split("&".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
              .map<Tuple2<String, String>> { keyValue ->
                  val split = keyValue.split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                  val key = split[0]
                  val value = decode(split[1])
                  Tuple2(key, value)
              }
              .foldLeft({ tree, tuple -> tree.update(tuple.a, { list -> list.cons(tuple.b) }, List.single(tuple.b)) }, TreeMap.empty(Ord.stringOrd))
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
