package org.kantega.niagara.http

interface EndpointResult<A> {

    val isMatched: Boolean
        get() = false


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

        fun <A> failed(): EndpointResult<A> {
            return Failed()
        }

        fun <A> notMatched(): EndpointResult<A> {
            return NotMatched()
        }

        fun <A> match(input: Input, output: Output<A>): EndpointResult<A> {
            return Matched(input, output)
        }
    }

}

data class Matched<A>(val input: Input, val value: Output<A>) : EndpointResult<A> {

    override val isMatched: Boolean
        get() = true

    override fun <B> fold(matched: (Matched<A>) -> B, notMatched: (NotMatched<A>) -> B, failed: (Failed<A>) -> B): B =
            matched(this)


    override fun toString(): String {
        return "Matched{" +
                "remainder=" + input +
                ", output=" + value +
                '}'.toString()
    }
}

class NotMatched<A> : EndpointResult<A> {

    override fun <B> fold(matched: (Matched<A>) -> B, notMatched: (NotMatched<A>) -> B, failed: (Failed<A>) -> B): B =
            notMatched(this)


    override fun toString(): String {
        return "NotMatched{}"
    }
}

class Failed<A> : EndpointResult<A> {

    override fun <B> fold(matched: (Matched<A>) -> B, notMatched: (NotMatched<A>) -> B, failed: (Failed<A>) -> B): B =
            failed(this)


    override fun toString(): String {
        return "Failed{}"
    }
}



