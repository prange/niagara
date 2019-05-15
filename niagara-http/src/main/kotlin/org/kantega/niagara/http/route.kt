package org.kantega.niagara.http


import io.vavr.collection.List
import io.vavr.control.Option
import io.vavr.kotlin.Try
import io.vavr.kotlin.component1
import io.vavr.kotlin.component2
import org.kantega.niagara.data.*
import org.kantega.niagara.http.RouteResult.Companion.failed
import org.kantega.niagara.http.RouteResult.Companion.match
import org.kantega.niagara.http.RouteResult.Companion.notMatched
import org.kantega.niagara.json.JsonDecoder
import org.kantega.niagara.json.JsonValue
import org.kantega.niagara.json.io.JsonParser
import kotlin.collections.dropLastWhile
import kotlin.collections.toTypedArray

data class RouteMatcher(val f: (Request) -> RouteResult<Request>) {

    fun match(request: Request): RouteResult<Request> =
      f(request)

    fun bind(g: (Request) -> RouteResult<Request>) =
      RouteMatcher { r -> f(r).flatMap { r2 -> g(r2) } }

    infix fun <A> thenExtract(f: (Request) -> RouteResult<P2<Request, A>>) = RouteExtractor { request ->
        RouteResult.match(request).fold(
          { match -> f(match.value) },
          { notMatched() },
          { fail -> Failed(fail.failure) }
        )
    }
}

data class RouteExtractor<A>(val f: (Request) -> RouteResult<P2<Request, A>>) {
    fun extract(request: Request): RouteResult<P2<Request, A>> =
      f(request)


    fun <B> map(f: (A) -> B): RouteExtractor<B> =
      RouteExtractor { input -> extract(input).map { (req, value) -> p(req, f(value)) } }

    fun <B> bind(f: (A) -> RouteResult<B>): RouteExtractor<B> =
      RouteExtractor { input -> extract(input).flatMap { (req, value) -> f(value).map({ b -> p(req, b) }) } }
}

interface Route<A> {


    fun handle(input: Request): RouteResult<P2<Request, A>>


    infix fun or(other: Route<A>): Route<A> {
        return OrEndpoint(this, other)
    }


    fun <B> bind(next: (A) -> Route<B>): Route<B> =
      ChainedEndpoints(this, next)

}

object Root : Route<HNil> {
    override fun handle(input: Request): RouteResult<P2<Request, HNil>> =
      match(p(input, HNil))
}

data class ExtractingRoute<A, HL : HList>(val extractor: RouteExtractor<A>, val rest: Route<HL>) : Route<HCons<A, HL>> {
    override fun handle(input: Request): RouteResult<P2<Request, HCons<A, HL>>> {
        val result = rest.handle(input)
        return result.flatMap { (tailremainder, tail) ->
            extractor.extract(tailremainder).map { (remainder, a) -> p(remainder, HCons(a, tail)) }
        }
    }
}

data class MatchingRoute<HL : HList>(val matcher: RouteMatcher, val rest: Route<HL>) : Route<HL> {
    override fun handle(input: Request): RouteResult<P2<Request, HL>> {
        val result = rest.handle(input)
        return result.flatMap { (tailremainder, tail) ->
            matcher.match(tailremainder).map { remainder -> p(remainder, tail) }
        }
    }
}

data class HandlingRoute<A, HL : HList>(val handler: (Request, HL) -> A, val tail: Route<HL>) : Route<A> {
    override fun handle(input: Request): RouteResult<P2<Request, A>> {
        val result = tail.handle(input)
        return result.map { (remainder, tail) -> p(remainder, handler(remainder, tail)) }
    }
}


data class OrEndpoint<A>(val one: Route<A>, val other: Route<A>) : Route<A> {

    override fun handle(input: Request): RouteResult<P2<Request, A>> {
        return one.handle(input).fold(
          { m -> m },
          { other.handle(input) },
          { f -> f }
        )
    }
}

data class ChainedEndpoints<A, B>(val first: Route<A>, val onSuccess: (A) -> Route<B>) : Route<B> {

    override fun handle(input: Request): RouteResult<P2<Request, B>> {
        return first.handle(input).flatMap({ res -> onSuccess(res._2).handle(res._1) })
    }
}

operator fun <A> Route<A>.plus(other: Route<A>): Route<A> =
  this or other


operator fun RouteMatcher.div(path: String): Route<HNil> =
  Root / this / path(path)

operator fun RouteMatcher.div(other: RouteMatcher): Route<HNil> =
  Root / this / other

operator fun <A> RouteMatcher.div(other: RouteExtractor<A>): Route<HCons<A, HNil>> =
  Root / this / other

operator fun <A> RouteExtractor<A>.div(other: RouteMatcher): Route<HCons<A, HNil>> =
  Root / this / other

operator fun <A, B> RouteExtractor<A>.div(other: RouteExtractor<B>): Route<HCons<B, HCons<A, HNil>>> =
  Root / this / other

operator fun <HL : HList> Route<HL>.div(matcher: RouteMatcher): Route<HL> =
  MatchingRoute(matcher, this)

operator fun <HL : HList> Route<HL>.div(path: String): Route<HL> =
  MatchingRoute(path(path), this)

operator fun <A, HL : HList> Route<HL>.div(extractor: RouteExtractor<A>): Route<HCons<A, HL>> =
  ExtractingRoute(extractor, this)


fun <A, HL : HList> Route<HL>.handler(handler: (Request, HL) -> A): Route<A> =
  HandlingRoute(handler, this)


operator fun <A> Route<HNil>.invoke(h: (Request) -> A): Route<A> =
  handler({ req, _ -> h(req) })

operator fun <A, B> Route<HCons<A, HNil>>.invoke(h: (Request, A) -> B): Route<B> =
  handler { req, hlist -> h(req, hlist.head) }

operator fun <A, B, C> Route<HCons<A, HCons<B, HNil>>>.invoke(h: (Request, A, B) -> C): Route<C> =
  handler { req, hlist -> h(req, hlist.head, hlist.tail.head) }

operator fun <A, B, C, D> Route<HCons<A, HCons<B, HCons<C, HNil>>>>.invoke(h: (Request, A, B, C) -> D): Route<D> =
  handler { req, hlist -> h(req, hlist.head, hlist.tail.head, hlist.tail.tail.head) }

operator fun <A, B, C, D, E> Route<HCons<A, HCons<B, HCons<C, HCons<D, HNil>>>>>.invoke(h: (Request, A, B, C, D) -> E): Route<E> =
  handler { req, hlist -> h(req, hlist.head, hlist.tail.head, hlist.tail.tail.head, hlist.tail.tail.tail.head) }

operator fun <A, B, C, D, E, F> Route<HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HNil>>>>>>.invoke(h: (Request, A, B, C, D, E) -> F): Route<F> =
  handler { req, hlist -> h(req, hlist.head, hlist.tail.head, hlist.tail.tail.head, hlist.tail.tail.tail.head, hlist.tail.tail.tail.tail.head) }

operator fun <A, B, C, D, E, F, G> Route<HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HNil>>>>>>>.invoke(h: (Request, A, B, C, D, E, F) -> G): Route<G> =
  handler { req, hlist -> h(req, hlist.head, hlist.tail.head, hlist.tail.tail.head, hlist.tail.tail.tail.head, hlist.tail.tail.tail.tail.head, hlist.tail.tail.tail.tail.tail.head) }

operator fun <A, B, C, D, E, F, G,H > Route<HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HCons<F, HCons<G, HNil>>>>>>>>.invoke(h: (Request, A, B, C, D, E, F, G) -> H): Route<H> =
  handler { req, hlist -> h(req, hlist.head, hlist.tail.head, hlist.tail.tail.head, hlist.tail.tail.tail.head, hlist.tail.tail.tail.tail.head, hlist.tail.tail.tail.tail.tail.head, hlist.tail.tail.tail.tail.tail.tail.head) }


val end =
  RouteMatcher { input ->
      if (input.remainingPath.isEmpty) match(input) else notMatched()
  }

val pathParam =
  RouteExtractor { input ->
      if (input.remainingPath.isEmpty)
          notMatched()
      else
          match(p(input.advancePath(1), input.remainingPath.head()))
  }

val optionalPathParam =
  RouteExtractor { input ->
      if (input.remainingPath.isEmpty)
          match(p(input, Option.none()))
      else
          match(p(input.advancePath(1), Option.of(input.remainingPath.head())))
  }

fun method(method: String): RouteMatcher =
  RouteMatcher { input ->
      if (input.method.toLowerCase() == method.toLowerCase())
          match(input)
      else
          notMatched()
  }


fun path(path: String): RouteMatcher =
  path(List.of(*path.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()))


fun path(path: List<String>): RouteMatcher =
  RouteMatcher { input ->
      if (path isPrefixOf input.remainingPath)
          match(input.advancePath(path.length()))
      else
          notMatched()
  }

val ROOT =
  Root

val GET =
  method("GET")

val POST =
  method("POST")

val PUT =
  method("PUT")


fun queryParam(name: String) =
  RouteExtractor { input ->
      input.queryParams
        .get(name)
        .flatMap { it.headOption() }
        .fold(
          { notMatched<P2<Request, String>>() },
          { n -> match(p(input, n)) }
        )
  }


fun queryParamAsInt(name: String) =
  RouteExtractor { input ->
      input.queryParams
        .get(name)
        .flatMap({ list -> if (!list.isEmpty) Try { list.head().toInt() }.toOption() else Option.none() })
        .fold(
          { notMatched<P2<Request, Int>>() },
          { n -> match(p(input, n)) }
        )
  }

fun queryParamAsLong(name: String) =
  RouteExtractor { input ->
      input.queryParams
        .get(name)
        .flatMap({ list -> if (!list.isEmpty) Try { list.head().toLong() }.toOption() else Option.none() })
        .fold(
          { notMatched<P2<Request, Long>>() },
          { n -> match(p(input, n)) }
        )
  }

fun mediaType(mediaType: MediaType) =
  RouteMatcher { request ->
      request
        .requestHeaders
        .get("Content-Type")
        .flatMap { list -> list.headOption() }
        .fold(
          { notMatched() },
          { headerValue -> if (headerValue.startsWith(mediaType.type)) match(request) else notMatched() }
        )
  }

val json =
  mediaType(APPLICATION_JSON)
    .thenExtract { input ->
        JsonParser.parse(input.body).fold(
          { nel -> failed<P2<Request, JsonValue>>(nel.toList().mkString(",")) },
          { json -> match(p(input, json)) })
    }

fun <A> json(decoder: JsonDecoder<A>): RouteExtractor<A> =
  json
    .bind { json ->
        decoder(json)
          .fold(
            { nel -> failed<A>(nel.toList().mkString(",")) },
            { v -> match(v) })
    }


fun <A> notFound(): Route<A> =
  NotFoundEndpoint()


fun <A> fail(reason: String): Route<A> =
  FailingEndpoint(reason)


fun <A> value(a: A) =
  RouteExtractor { input -> match(p(input, a)) }

fun <A> routes(vararg routes: Route<A>): Route<A> =
  List.of(*routes).foldLeft(notFound(), { accum, route -> accum or route })

class NotFoundEndpoint<A> : Route<A> {
    override fun handle(input: Request): RouteResult<P2<Request, A>> =
      notMatched()

    override fun or(other: Route<A>): Route<A> =
      other
}

data class FailingEndpoint<A>(val reason: String) : Route<A> {
    override fun handle(input: Request): RouteResult<P2<Request, A>> =
      failed(reason)
}





