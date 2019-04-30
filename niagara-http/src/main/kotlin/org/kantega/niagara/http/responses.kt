package org.kantega.niagara.http

import io.vavr.collection.TreeMap
import io.vavr.control.Option

fun Ok(): Response {
    return Response(TreeMap.empty(), TreeMap.empty(), 200, Option.none())
}

fun Ok(body: String): Response {
    return Ok().withBody(body)
}

fun body(value: String): Response {
    return Ok().withBody(value)
}


fun resource() = ""