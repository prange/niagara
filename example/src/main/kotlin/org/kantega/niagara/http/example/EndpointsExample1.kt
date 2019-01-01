package org.kantega.niagara.http.example

import org.kantega.niagara.http.*

object EndpointsExample1 {

    @JvmStatic
    fun main(args: Array<String>) {

        //Define the endpoint
        val endpoint = all(
          get("/")(Ok("This is the root")),
          get("/ping")(Ok("Pong")),
          (get / "ping" / "ling") { Ok("nested") },
          (get("/jalla", pathParam)){ id -> Ok("The id was $id") }
        )

        //Test the endpoint
        val request = Input.get("/jalla/6")

        val response = endpoint.handle(request)

        val result = response.fold(
          { ok -> ok.value },
          { _ -> "Not matched" },
          { _ -> "Endpoint invocation failed" }
        )

        println(result)


    }
}
