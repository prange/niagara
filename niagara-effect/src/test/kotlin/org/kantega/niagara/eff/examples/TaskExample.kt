package org.kantega.niagara.eff.examples

import org.kantega.niagara.eff.Task
import org.kantega.niagara.eff.runTask


fun main(){


    val value = Task("String")
    val effect = Task{"Jalla"}
    val print = {s:String->Task{println(s)}}

    val bound =
      value
        .bind { s->effect.map { e -> s+ e  } }
        .bind(print)

    runTask(bound)
}