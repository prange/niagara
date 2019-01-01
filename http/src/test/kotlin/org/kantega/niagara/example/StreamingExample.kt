package org.kantega.niagara.example

import org.kantega.niagara.stream.Fold
import org.kantega.niagara.task.Console
import org.kantega.niagara.task.TaskExecutor

fun main(args: Array<String>) {

    val tr = TaskExecutor.defaultExecutor

    val stream =
            Fold.output("a", "b", "c")
                    .map { s -> s.toUpperCase() }
                    .sink(Console::outputln,tr)

    tr.eval(stream.compile())
}