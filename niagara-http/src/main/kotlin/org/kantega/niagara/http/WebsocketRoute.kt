package org.kantega.niagara.http

/*
import arrow.core.getOrElse
import arrow.core.toOption
import fj.Equal
import io.undertow.websockets.core.AbstractReceiveListener
import io.undertow.websockets.core.BufferedTextMessage
import io.undertow.websockets.core.WebSocketChannel
import io.undertow.websockets.core.WebSockets
import io.undertow.websockets.core.protocol.Handshake
import io.undertow.websockets.core.protocol.version07.Hybi07Handshake
import io.undertow.websockets.core.protocol.version08.Hybi08Handshake
import io.undertow.websockets.core.protocol.version13.Hybi13Handshake
import io.undertow.websockets.spi.AsyncWebSocketHttpServerExchange
import org.jctools.queues.MpscArrayQueue
import org.kantega.niagara.http.*
import org.kantega.niagara.stream.Fold
import org.kantega.niagara.stream.QueueFold
import org.kantega.niagara.task.Task
import org.kantega.niagara.task.TaskExecutor
import java.io.IOException
import java.util.*

class WebsocketUpgradeEndpoint(private val wrapped: StreamingEndpoint) : Route<Entity> {

    private val handshakes = listOf(Hybi13Handshake(), Hybi08Handshake(), Hybi07Handshake())

    override fun handle(input: Request): RouteResult<Entity> =
      if (!Equal.stringEqual.eq(input.method, "GET"))
          NotMatched()
      else
          input.maybeServerConnection.flatMap { exchange ->
              val facade = AsyncWebSocketHttpServerExchange(exchange, connected)

              val maybeHandshaker = handshakes.find { hs -> hs.matches(facade) }.toOption()

              maybeHandshaker
                .map { hs:Handshake ->
                    exchange.upgradeChannel { connx, _ ->
                        val channel = hs.createChannel(facade, connx, facade.bufferPool)
                        connected.add(channel)
                        val planQueue = PlanExchange()
                        val incomingplan = QueueFold<String>(planQueue.incoming)
                        val wsInput = StreamingInput(incomingplan)
                        val wsOutput = wrapped(wsInput)
                        val outputplan = wsOutput.output.sink({ str: String -> Task.invoke { WebSockets.sendTextBlocking(str, channel) } }, TaskExecutor.defaultExecutor)
                        TaskExecutor.defaultExecutor.eval(outputplan.compile())
                        channel.receiveSetter.set(planQueue)
                    }
                    hs.handshake(facade)
                    RouteResult.match<Entity>(input, Response.fromExchange(exchange).withBody(EmptyResponse))
                }
          }.getOrElse { NotMatched() }


    internal class PlanExchange : AbstractReceiveListener() {

        val incoming = MpscArrayQueue<String>(1000)

        @Throws(IOException::class)
        override fun onFullTextMessage(channel: WebSocketChannel?, message: BufferedTextMessage?) {
            incoming.offer(message!!.data)
        }
    }

    companion object {

        private val connected = HashSet<WebSocketChannel>()

        fun se(sep: StreamingEndpoint): Route<Entity> =
          WebsocketUpgradeEndpoint(sep)



    }
}
fun ws(stream: (Fold<String>) -> Fold<String>): Route<Entity> =
  WebsocketUpgradeEndpoint.se({ input: StreamingInput ->
      StreamingOutput(stream(input.input))
  })
*/