package org.kantega.niagara.http

interface RouteResult<A> {



    fun <B> map(f: (A) -> B): RouteResult<B> =
      fold({ m -> Matched(f(m.value)) }, { NotMatched() }, { Failed() })

    fun <B> flatMap(f:(A)->RouteResult<B>) =
      fold({m->f(m.value)},{ NotMatched() }, { Failed() })

    fun <B> fold(matched: (Matched<A>) -> B, notMatched: (NotMatched<A>) -> B, failed: (Failed<A>) -> B): B

    fun run(onMatch: (Matched<A>) -> Unit, onNotMatched: (NotMatched<A>) -> Unit, onFailed: (Failed<A>) -> Unit) {
        val r = fold(
          { match -> { onMatch(match) } },
          { noMatch -> { onNotMatched(noMatch) } },
          { fail -> { onFailed(fail) } }
        )
        r()
    }

    companion object {

        fun <A> failed(): RouteResult<A> {
            return Failed()
        }

        fun <A> notMatched(): RouteResult<A> {
            return NotMatched()
        }

        fun <A> match(input: A): RouteResult<A> {
            return Matched(input)
        }
    }

}

data class Matched<A>(val value: A) : RouteResult<A> {


    override fun <B> fold(matched: (Matched<A>) -> B, notMatched: (NotMatched<A>) -> B, failed: (Failed<A>) -> B): B =
      matched(this)



}

class NotMatched<A> : RouteResult<A> {

    override fun <B> fold(matched: (Matched<A>) -> B, notMatched: (NotMatched<A>) -> B, failed: (Failed<A>) -> B): B =
      notMatched(this)


    override fun toString(): String {
        return "NotMatched()"
    }
}

class Failed<A> : RouteResult<A> {

    override fun <B> fold(matched: (Matched<A>) -> B, notMatched: (NotMatched<A>) -> B, failed: (Failed<A>) -> B): B =
      failed(this)


    override fun toString(): String {
        return "Failed()"
    }
}



