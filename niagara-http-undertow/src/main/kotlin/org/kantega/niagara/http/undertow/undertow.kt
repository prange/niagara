package org.kantega.niagara.http.undertow

import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.CookieImpl
import io.undertow.util.HeaderMap
import io.undertow.util.HttpString
import io.vavr.collection.TreeMap
import io.vavr.control.Option
import org.kantega.niagara.data.update
import org.kantega.niagara.http.Cookie
import org.kantega.niagara.http.Request
import org.kantega.niagara.http.Response
import org.kantega.niagara.http.SameSiteMode
import java.io.InputStream
import java.net.URI
import java.util.*
import io.undertow.server.handlers.Cookie as UCookie
import io.vavr.collection.List
import java.io.IOException

fun fromExchange(exchange: HttpServerExchange) =
  Request(
    TreeMap.ofAll(exchange.requestCookies).mapValues { convertCookie(it) },
    toMap(exchange.requestHeaders),
    TreeMap.ofAll<String, Deque<String>>(exchange.queryParameters).mapValues({ d -> List.ofAll(d) }),
    readBytesToString(exchange.inputStream),
    URI.create(exchange.requestURI),
    List.of(*exchange.requestPath.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()),
    List.empty(),
    exchange.requestMethod.toString())


fun intoExcehange(response: Response, exchange: HttpServerExchange) {
    response.responseCookies.forEach { key, cookie ->
        exchange.responseCookies[key] = convertCookie(cookie)
    }

    response.responseHeaders.forEach { name, values ->
        values.forEach { value -> exchange.responseHeaders.add(HttpString(name), value) }
    }

    exchange.statusCode = response.statusCode

    response.body.forEach { body ->
        val readAllBytes = body.readBytes()
        exchange.responseContentLength = readAllBytes.size.toLong()
        exchange.outputStream.write(readAllBytes)
    }
}

fun convertCookie(ucookie: UCookie): Cookie =
  Cookie(
    ucookie.name,
    ucookie.value,
    Option.of(ucookie.path),
    Option.of(ucookie.domain),
    Option.of(ucookie.maxAge),
    Option.of(ucookie.expires),
    ucookie.isDiscard,
    ucookie.isSecure,
    ucookie.isHttpOnly,
    ucookie.version,
    Option.of(ucookie.comment),
    ucookie.isSameSite,
    Option.of(ucookie.sameSiteMode).map { ssm -> if (ssm == "Lax") SameSiteMode.Lax else if (ssm == "Strict") SameSiteMode.Strict else SameSiteMode.None }.getOrElse(SameSiteMode.None)
  )

fun convertCookie(cookie: Cookie): UCookie {
    val ucookie = CookieImpl(cookie.name, cookie.value)
    ucookie.path = cookie.path.orNull
    ucookie.domain = cookie.domain.orNull
    ucookie.maxAge = cookie.maxAge.orNull
    ucookie.expires = cookie.expires.orNull
    ucookie.isDiscard = cookie.discard
    ucookie.isSecure = cookie.secure
    ucookie.isHttpOnly = cookie.httpOnly
    ucookie.version = cookie.version
    ucookie.comment = cookie.comment.orNull
    ucookie.isSameSite = cookie.sameSite
    ucookie.sameSiteMode = if (cookie.sameSiteMode == SameSiteMode.None) null else cookie.sameSiteMode.name

    return ucookie
}

private fun readBytesToString(stream: InputStream): String =
  stream.bufferedReader().use { it.readText() }


private fun toMap(headerMap: HeaderMap): TreeMap<String, List<String>> =
  headerMap.headerNames.fold(
    TreeMap.empty<String, List<String>>(),
    { map, name ->
        headerMap
          .get(name)
          .fold(
            map,
            { updatedMap, value ->
                updatedMap.update(name.toString(), { list -> list.append(value) }, { List.of(value) })
            })
    })



