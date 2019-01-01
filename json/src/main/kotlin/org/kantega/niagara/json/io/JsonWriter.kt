package org.kantega.niagara.json.io


import io.vavr.collection.List
import io.vavr.collection.Stream
import org.kantega.niagara.json.JsonValue
import java.io.StringWriter

object JsonWriter {
    private val CONTROL_CHARACTERS_END = 0x001f

    private val QUOT_CHARS = charArrayOf('\\', '"')
    private val BS_CHARS = charArrayOf('\\', '\\')
    private val LF_CHARS = charArrayOf('\\', 'n')
    private val CR_CHARS = charArrayOf('\\', 'r')
    private val TAB_CHARS = charArrayOf('\\', 't')
    // In JavaScript, U+2028 and U+2029 characters count as line endings and must be encoded.
    // http://stackoverflow.com/questions/2965293/javascript-parse-error-on-u2028-unicode-character
    private val UNICODE_2028_CHARS = charArrayOf('\\', 'u', '2', '0', '2', '8')
    private val UNICODE_2029_CHARS = charArrayOf('\\', 'u', '2', '0', '2', '9')
    private val HEX_DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')


    fun write(json: JsonValue): String =
      json
        .fold(
          { "null" },
          { bool -> if (bool.value) "true" else "false" },
          { num -> num.n.toString() },
          { str -> "\""+writeJsonString(str.s)+"\"" },
          { obj -> mkString(obj.m.toList().map({ pairs -> "\"" + pairs._1() + "\":" + write(pairs._2()) }), "{", ",", "}") },
          { arr -> if (arr.a.isEmpty) "[]" else "[" + arr.a.tail().foldLeft(write(arr.a.head()), { sum, v -> sum + "," + write(v) }) + "]" }
        )


    fun writePretty(json: JsonValue): String =
      writePretty(json, 4)


    fun writePretty(json: JsonValue, indent: Int): String =
      json
        .fold(
          { "null" },
          { bool -> if (bool.value) "true" else "false" },
          { num -> num.n.toString() },
          { str -> "\""+writeJsonString(str.s)+"\"" },
          { obj -> mkString(obj.m.toList().map({ pairs -> indent(indent + 2) + "\"" + pairs._1() + "\":" + writePretty(pairs._2(), indent + 2) }), line("{"), ",\n", "\n" + indent(indent) + "}") },
          { arr -> if (arr.a.isEmpty) "[]" else "[" + arr.a.tail().foldLeft(indent(indent + 2) + writePretty(arr.a.head(), indent + 2) + line("") + indent(indent) , { sum, v -> sum + line(",") + indent(indent + 2) + writePretty(v, indent + 2) }) + "]" }
        )


    private fun mkString(vals: List<String>, pre: String, delim: String, post: String): String {
        return if (vals.isEmpty)
            pre + post
        else
            pre + vals.tail().foldLeft(vals.head(), { sum, v -> sum + delim + v }) + post
    }

    private fun indent(depth: Int): String {
        return " ".repeat(depth)
    }

    private fun line(l: String): String {
        return l + "\n"
    }


    private fun writeJsonString(string: String): String {
        try {
            val writer = StringWriter()
            val length = string.length
            var start = 0
            for (index in 0 until length) {
                val replacement = getReplacementChars(string[index])
                if (replacement != null) {
                    writer.write(string, start, index - start)
                    writer.write(replacement)
                    start = index + 1
                }
            }
            writer.write(string, start, length - start)
            return writer.toString()
        } catch (e: Exception) {
            throw RuntimeException("Could not write string" + e.message, e)
        }

    }


    private fun getReplacementChars(ch: Char): CharArray? {
        if (ch > '\\') {
            if (ch < '\u2028' || ch > '\u2029') {
                // The lower range contains 'a' .. 'z'. Only 2 checks required.
                return null
            }
            return if (ch == '\u2028') UNICODE_2028_CHARS else UNICODE_2029_CHARS
        }
        if (ch == '\\') {
            return BS_CHARS
        }
        if (ch > '"') {
            // This range contains '0' .. '9' and 'A' .. 'Z'. Need 3 checks to get here.
            return null
        }
        if (ch == '"') {
            return QUOT_CHARS
        }
        if (ch.toInt() > CONTROL_CHARACTERS_END) {
            return null
        }
        if (ch == '\n') {
            return LF_CHARS
        }
        if (ch == '\r') {
            return CR_CHARS
        }
        return if (ch == '\t') {
            TAB_CHARS
        } else charArrayOf('\\', 'u', '0', '0', HEX_DIGITS[ch.toInt() shr 4 and 0x000f], HEX_DIGITS[ch.toInt() and 0x000f])
    }
}
