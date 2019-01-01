package org.kantega.niagara.task


object Console {

    fun outputln(line: String): Task<Unit> {
        return Task { println(line) }
    }

}
