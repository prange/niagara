package org.kantega.niagara.http.undertow


import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Methods
import io.undertow.websockets.core.*
import io.undertow.websockets.core.protocol.Handshake
import io.undertow.websockets.core.protocol.version07.Hybi07Handshake
import io.undertow.websockets.core.protocol.version08.Hybi08Handshake
import io.undertow.websockets.core.protocol.version13.Hybi13Handshake
import io.undertow.websockets.spi.AsyncWebSocketHttpServerExchange
import org.kantega.niagara.Task
import org.jctools.queues.MpscArrayQueue
import org.kantega.niagara.Source
import org.kantega.niagara.data.toOption
import org.kantega.niagara.http.*
import org.kantega.niagara.runTask
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import io.undertow.websockets.core.WebSocketChannel
import org.kantega.niagara.enqueue
import java.util.Collections.newSetFromMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicBoolean


class NiagaraWebsocketHandler(
  val routes: Route<WebsocketResponse>,
  val executorService: ScheduledExecutorService) : HttpHandler {

    private val handshakes = listOf(Hybi13Handshake(), Hybi08Handshake(), Hybi07Handshake())

    override fun handleRequest(exchange: HttpServerExchange) {
        if (!exchange.requestMethod.equals(Methods.GET)) {
            // Only GET is supported to start the handshake
            exchange.statusCode = 404
            exchange.endExchange()
            return
        }

        val request = fromExchange(exchange)
        val result = routes.handle(request)

        result.run(
          { outputMatched ->
              val facade = AsyncWebSocketHttpServerExchange(exchange, connected)

              val maybeHandshaker =
                handshakes
                  .find { hs -> hs.matches(facade) }
                  .toOption()

              maybeHandshaker
                .onEmpty {
                    exchange.statusCode = 404
                    exchange.endExchange()
                }
                .forEach { hs: Handshake ->
                    exchange.upgradeChannel { connx, _ ->
                        val incoming = MpscArrayQueue<String>(1000)
                        val closeSignal = AtomicBoolean(false)
                        val channel = hs.createChannel(facade, connx, facade.bufferPool)
                        connected.add(channel)
                        channel.close()

                        channel.closeSetter.set { }
                        val planQueue = IncomingQueue(enqueue(incoming),{ Task.exec { closeSignal.set(true) }},executorService)
                        channel.receiveSetter.set(planQueue)
                        val outbound = outputMatched.value._2.streams(Source.queue(incoming))
                        val outboundSource = outbound.into({ str: String -> Task.exec { WebSockets.sendTextBlocking(str, channel) } })
                        runTask(outboundSource.compile(),executorService)
                    }
                    hs.handshake(facade)
                }
          },
          {
              exchange.statusCode = 404
              exchange.endExchange()
          },
          { outputFailed ->
              throw RuntimeException(outputFailed.toString())
          }
        )


    }

    companion object {

        private val connected = newSetFromMap(ConcurrentHashMap<WebSocketChannel, Boolean>())


    }
}

data class IncomingQueue(
  val onMessageTask:(String)-> Task<Unit>,
  val closeTask: (WebSocketChannel) -> Task<Unit>,
  val executorService: ScheduledExecutorService) : AbstractReceiveListener() {



    @Throws(IOException::class)
    override fun onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage) {
        runTask(onMessageTask(message.data))
    }

    override fun onClose(webSocketChannel: WebSocketChannel?, channel: StreamSourceFrameChannel?) {
        runTask(closeTask(webSocketChannel!!),executorService)
    }
}



