package org.kantega.niagara.json

import org.kantega.niagara.data.curried


fun main() {
    val user = jLift(::User.curried()) apply
      jOk("Ola") apply
      jFail("Too old") apply
      jOk("ghy")

    println(user)


}


data class User(val name: String, val age: Int, val email: String)
