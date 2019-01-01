package org.kantega.niagara.http

import fj.Equal.stringEqual
import fj.data.List
import fj.data.Option
import org.kantega.niagara.http.EndpointResult.Companion.failed
import org.kantega.niagara.http.EndpointResult.Companion.match
import org.kantega.niagara.http.EndpointResult.Companion.notMatched
import java.util.*

interface Endpoint<A> {


    fun handle(input: Input): EndpointResult<A>

    fun end(): Endpoint<A> =
            Endpoint { input ->
                this@Endpoint.handle(input).fold(
                        { aMatched -> if (aMatched.input.remainingPath.isEmpty) aMatched else notMatched<A>() },
                        { aNotMatched -> aNotMatched },
                        { aFailed -> aFailed }
                )
            }


    /**
     * Tries to mach the first endpoint, if no match, tries the second IN ORDER.
     *
     * @param other the other
     * @return a new endpoint
     */
    fun or(other: Endpoint<A>): Endpoint<A> {
        return OrEndpoint(this, other)
    }

    /**
     * If the endpoint matches, goes for the next, but discarding the output of this.
     *
     * @param next the next
     * @param <B>  the type og the return
     * @return a new enpoint
    </B> */
    fun <B> then(next: Endpoint<B>): Endpoint<B> =
            then(next, { _, b -> b })

    operator fun <B> div(next: Endpoint<B>): Endpoint<B> =
        then(next, { _, b -> b })

    operator fun div(path: String): Endpoint<Unit> =
            then(path(path))


    operator fun invoke(a: Output<Entity>): Endpoint<Entity> =
            then(value(a)).end()


    operator fun invoke(f: (A) -> Output<Entity>): Endpoint<Entity> =
            bind({ a -> value(f(a)) }).end()


    /**
     * If the endpoint matches, goes for the next, joining the outputs if both match
     *
     * @param next the next
     * @param <B>  the type og the return
     * @return a new enpoint
    </B> */
    fun <B, C> then(next: Endpoint<B>, f: (A, B) -> C): Endpoint<C> =
            bind({ a -> next.map({ b -> f(a, b) }) })


    fun <B> bind(next: (A) -> Endpoint<B>): Endpoint<B> =
            ChainedEndpoints(this, next)


    fun <B> map(f: (A) -> B): Endpoint<B> =
            bind({ a -> value(f(a)) })


    class OrEndpoint<A>(val one: Endpoint<A>, val other: Endpoint<A>) : Endpoint<A> {

        override fun handle(input: Input): EndpointResult<A> {
            return one.handle(input).fold(
                    { m -> m },
                    { _ -> other.handle(input) },
                    { f -> f }
            )
        }
    }

    class ChainedEndpoints<A, B>(val first: Endpoint<A>, val onSuccess: (A) -> Endpoint<B>) : Endpoint<B> {

        override fun handle(input: Input): EndpointResult<B> {
            return first.handle(input).fold(
                    { match -> onSuccess(match.value.value).handle(match.input) },
                    { _ -> notMatched() },
                    { _ -> failed() }
            )
        }
    }

    companion object {

        operator fun <A> invoke(f: (Input) -> EndpointResult<A>): Endpoint<A> =
                WrappedEndpoint(f)


    }

}

val pathParam =
        Endpoint { input ->
            if (input.remainingPath.isEmpty)
                notMatched()
            else
                match(input.advancePath(1), Ok().withBody<String>(input.remainingPath.head()))
        }

val optionalPathParam =
        Endpoint { input ->
            if (input.remainingPath.isEmpty)
                match(input, body(Optional.empty()))
            else
                match(input.advancePath(1), body(Optional.ofNullable(input.remainingPath.head())))
        }

fun get(): Endpoint<Unit> =
        Endpoint { input ->
            if (stringEqual.eq(input.method, "GET"))
                match(input, body(Unit))
            else
                notMatched()
        }

fun path(path: String): Endpoint<Unit> =
        path(List.arrayList(*path.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()))


fun path(path: List<String>): Endpoint<Unit> =
        Endpoint { input ->
            if (path.isPrefixOf(stringEqual, input.remainingPath))
                match(input.advancePath(path.length()), body(Unit))
            else
                notMatched()
        }

val ROOT =
        path("/")

val get =
        get()

fun get(path: String): Endpoint<Unit> {
    return get().then(path(path))
}


fun <A> get(path: String, next: Endpoint<A>): Endpoint<A> {
    return get(path).then(next)
}

fun <A, B> get(path: String, next: Endpoint<A>, handler: (A) -> Endpoint<B>): Endpoint<B> {
    return get(path).then(next).bind(handler)
}

fun <A> get(wrapped: Endpoint<A>): Endpoint<A> {
    return get().then(wrapped)
}

fun queryParam(name: String): Endpoint<String> =
        Endpoint { input ->
            input.queryParams.get(name).option(
                    notMatched(),
                    { nel -> match(input, body<String>(nel.head())) }
            )
        }


fun queryParamAsInt(name: String): Endpoint<Int> =
        Endpoint { input ->
            input.queryParams.get(name).bind({ list -> if (list.isNotEmpty) Option.parseInt.f(list.head()) else Option.none() }
            ).option(
                    notMatched(),
                    { n -> match(input, body<Int>(n)) }
            )
        }

fun queryParamAsLong(name: String): Endpoint<Long> =
  Endpoint { input ->
      input.queryParams.get(name).bind({ list -> if (list.isNotEmpty) Option.parseLong.f(list.head()) else Option.none() }
      ).option(
        notMatched(),
        { n -> match(input, body<Long>(n)) }
      )
  }


fun <A> queryParamAsInt(name: String, handler: (Int) -> Endpoint<A>): Endpoint<A> =
        queryParamAsInt(name).bind(handler)

fun <A> queryParamAsLong(name: String, handler: (Long) -> Endpoint<A>): Endpoint<A> =
  queryParamAsLong(name).bind(handler)

fun all(vararg eps: Endpoint<Entity>): Endpoint<Entity> =
        eps.foldRight(notFound(), { obj, other -> obj.or(other) })


fun <A> notFound(): Endpoint<A> =
        NotFoundEndpoint()


fun <A> fail(): Endpoint<A> =
        FailingEndpoint()


fun entity(a: String): Endpoint<Entity> =
        MatchingEndpoint(Ok(a))


fun <A> value(a: A): Endpoint<A> =
        MatchingEndpoint(body(a))

fun <A> value(a: Output<A>): Endpoint<A> =
        MatchingEndpoint(a)

data class WrappedEndpoint<A>(val f: (Input) -> EndpointResult<A>) : Endpoint<A> {
    override fun handle(input: Input): EndpointResult<A> =
            f(input)
}

data class MatchingEndpoint<A>(val a: Output<A>) : Endpoint<A> {
    override fun handle(input: Input): EndpointResult<A> =
            match(input, a)
}

class NotFoundEndpoint<A> : Endpoint<A> {
    override fun handle(input: Input): EndpointResult<A> =
            notMatched()
}

class FailingEndpoint<A> : Endpoint<A> {
    override fun handle(input: Input): EndpointResult<A> =
            failed()
}


