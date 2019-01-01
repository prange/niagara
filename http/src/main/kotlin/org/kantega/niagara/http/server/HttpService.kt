package org.kantega.niagara.http.server

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import org.kantega.niagara.http.Endpoint
import org.kantega.niagara.http.Entity
import org.kantega.niagara.http.Input

class HttpService(val endpoint: Endpoint<Entity>) : HttpHandler {

    @Throws(Exception::class)
    override fun handleRequest(exchange: HttpServerExchange) {
        val output = endpoint.handle(Input.of(exchange))
        output.run(
                { outputMatched ->
                    exchange.statusCode = outputMatched.value.statusCode
                    outputMatched.value.value.body().ifPresent { body ->
                        exchange.responseContentLength = body.length.toLong()
                        exchange.outputStream.write(body.toByteArray())
                    }
                },
                { _ ->
                    exchange.statusCode = 404
                    exchange.endExchange()
                },
                { outputFailed ->
                    throw RuntimeException(outputFailed.toString()) }

        )
    }
}
