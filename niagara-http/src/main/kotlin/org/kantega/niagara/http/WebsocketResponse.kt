package org.kantega.niagara.http

import io.vavr.collection.List
import io.vavr.collection.TreeMap
import org.kantega.niagara.eff.Pipe

data class WebsocketResponse(
  val responseCookies: TreeMap<String, Cookie>,
  val responseHeaders: TreeMap<String, List<String>>,
  val statusCode: Int,
  val streams: Pipe<String, String>)