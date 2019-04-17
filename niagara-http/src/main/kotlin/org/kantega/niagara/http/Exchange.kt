package org.kantega.niagara.http

data class Exchange<A>(val request: Request, val response: Response, val value: A) {


    fun <B> map(f: (A) -> B) =
      Exchange(request, response, f(value))

}