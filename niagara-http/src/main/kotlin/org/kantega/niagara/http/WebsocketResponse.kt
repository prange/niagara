package org.kantega.niagara.http

import io.vavr.collection.List
import io.vavr.collection.TreeMap
import io.vavr.control.Option
import org.kantega.niagara.Pipe

data class WebsocketResponse(
  val responseCookies: TreeMap<String, Cookie>,
  val responseHeaders: TreeMap<String, List<String>>,
  val statusCode: Int,
  val streams: Pipe<String,String>)