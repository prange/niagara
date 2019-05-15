package org.kantega.niagara.http.undertow

import io.undertow.Undertow
import org.kantega.niagara.eff.Task
import org.kantega.niagara.eff.liftToTask
import org.kantega.niagara.eff.runTask
import org.kantega.niagara.http.*
import java.util.concurrent.Executors


val ping =
  (GET / "ping"){ Task(Ok("pong"))}


val echo =
  (GET / "echo" / queryParam("input")){_,i -> Task(Ok(i))}

val assets =
  (GET / "asset")(classPathResources("assets").liftToTask())


fun main() {
    val executor =
      Executors.newSingleThreadScheduledExecutor()

    val routes =
      ping + echo + assets

    val server =
      Undertow.builder()
        .addHttpListener(8081, "localhost")
        .setHandler(NiagaraTaskHttpHandler(routes, { task -> runTask(task, executor) })).build()

    server.start()
}