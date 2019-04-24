package org.kantega.niagara.http.undertow

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import org.kantega.niagara.http.Response
import org.kantega.niagara.http.Route

class NiagaraHttpHandler(val route: Route<Response>) : HttpHandler {

    @Throws(Exception::class)
    override fun handleRequest(exchange: HttpServerExchange) {
        val output = route.handle(fromExchange(exchange))
        output.run(
          { outputMatched ->
              intoExcehange(outputMatched.value._2,exchange)
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