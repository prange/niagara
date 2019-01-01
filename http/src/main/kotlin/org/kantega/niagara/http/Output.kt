package org.kantega.niagara.http

import fj.Ord
import fj.Unit
import fj.data.TreeMap
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.Cookie
import io.undertow.util.HeaderMap
import io.undertow.util.HttpString

class Output<A>(val responseCookies: TreeMap<String, Cookie>, val responseHeaders: HeaderMap, val statusCode: Int, val value: A) {

    fun withHeader(key: HttpString, headerValue: String): Output<A> {
        return Output(responseCookies, responseHeaders.add(key, headerValue), statusCode, value)
    }

    fun withStatusCode(statusCode: Int): Output<A> {
        return Output(responseCookies, responseHeaders, statusCode, value)
    }

    fun <B> withBody(value: B): Output<B> {
        return Output(responseCookies, responseHeaders, statusCode, value)
    }

    override fun toString(): String {
        return "Output{" +
                "responseCookies=" + responseCookies +
                ", responseHeaders=" + responseHeaders +
                ", statusCode=" + statusCode +
                ", value=" + value +
                '}'.toString()
    }

    companion object {

        fun fromExchange(exchange: HttpServerExchange): Output<String> {

            return Output(TreeMap.fromMutableMap(Ord.stringOrd,exchange.responseCookies),exchange.responseHeaders,exchange.statusCode,"")
        }


    }
}

fun Ok(): Output<Entity> {
    return Output(TreeMap.empty(Ord.stringOrd), HeaderMap(), 200, EmptyResponse)
}

fun Ok(body: String): Output<Entity> {
    return Ok().withBody(BodyResponse(body))
}

fun <A> body(value: A): Output<A> {
    return Ok().withBody(value)
}