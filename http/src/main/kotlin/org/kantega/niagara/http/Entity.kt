package org.kantega.niagara.http

import java.util.Optional

interface Entity {

    fun body(): Optional<String>

}

object EmptyResponse : Entity{
    override fun body(): Optional<String> =
            Optional.empty()

}

data class BodyResponse(val body:String) :Entity{
    override fun body(): Optional<String> =
            Optional.ofNullable(body)
}
