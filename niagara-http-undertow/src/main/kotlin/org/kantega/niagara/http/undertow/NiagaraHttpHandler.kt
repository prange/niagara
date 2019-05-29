package org.kantega.niagara.http.undertow

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import org.kantega.niagara.eff.Task
import org.kantega.niagara.http.Response
import org.kantega.niagara.http.Route

class NiagaraHttpHandler(val route: Route<Response>) : HttpHandler {

    @Throws(Exception::class)
    override fun handleRequest(exchange: HttpServerExchange) {
        val output = route.handle(fromExchange(exchange))
        output.run(
          { outputMatched ->
              intoExcehange(outputMatched.value._2, exchange)
              exchange.endExchange()
          },
          { _ ->
              exchange.statusCode = 404
              exchange.endExchange()
          },
          { outputFailed ->
              exchange.statusCode = 500
              exchange.endExchange()
              throw RuntimeException(outputFailed.toString())
          }

        )
    }
}

class NiagaraTaskHttpHandler(val route: Route<Task<Response>>, val executor: (Task<*>) -> Unit) : HttpHandler {

    @Throws(Exception::class)
    override fun handleRequest(exchange: HttpServerExchange) {
        val runnable:()->Unit =
          {
            executor(
              Task {
                  exchange.startBlocking()
                  val req = fromExchange(exchange)
                  val output = route.handle(req)
                  output.run(
                    { outputMatched ->
                        executor(outputMatched.value._2
                          .handle { t ->
                              Task {
                                  t.printStackTrace()
                                  Response(statusCode = 500)
                              }
                          }
                          .bind { resp ->
                              Task {
                                  intoExcehange(resp, exchange)
                                  exchange.endExchange()
                              }
                          })
                    },
                    { _ ->
                        exchange.statusCode = 404
                        exchange.endExchange()
                    },
                    { outputFailed ->
                        exchange.endExchange()
                        println(outputFailed.failure)
                        throw RuntimeException(outputFailed.toString())
                    }

                  )
              }
            )}
        exchange.dispatch(runnable)
    }
}