package org.kantega.niagara.json.io

class ParseFailure(message: String, val offset: Int, val line: Int, val i: Int) : RuntimeException(message)
