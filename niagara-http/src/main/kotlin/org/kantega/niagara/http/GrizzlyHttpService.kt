package org.kantega.niagara.http

/*
class HttpService(val endpoint: Route<Entity>) : HttpHandler {

    @Throws(Exception::class)
    override fun handleRequest(exchange: HttpServerExchange) {
        val output = endpoint.handle(Request.of(exchange))
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
*/
