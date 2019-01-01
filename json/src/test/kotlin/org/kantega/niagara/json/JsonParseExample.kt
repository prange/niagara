package org.kantega.niagara.json

import arrow.syntax.function.curried

fun main(args: Array<String>) {
    val user = jLift(::User.curried()) apply
      jOk("Ola") apply
      jFail("Too old") apply
      jOk("ghy")

    println(user)


}


data class User(val name: String, val age: Int, val email: String)
