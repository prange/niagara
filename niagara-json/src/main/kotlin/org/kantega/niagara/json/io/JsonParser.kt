package org.kantega.niagara.json.io


import io.vavr.collection.TreeMap
import io.vavr.collection.List
import org.kantega.niagara.json.*

import java.io.IOException
import java.io.Reader
import java.io.StringReader
import java.math.BigDecimal


/**
 * Parser, based on the fast https://github.com/ralfstx/minimal-json
 */
class JsonParser private constructor(private val reader: Reader, buffersize: Int = DEFAULT_BUFFER_SIZE) {
    private val buffer: CharArray
    private var bufferOffset: Int = 0
    private var index: Int = 0
    private var fill: Int = 0
    private var line: Int = 0
    private var lineOffset: Int = 0
    private var current: Char = '0'
    private var captureBuffer: StringBuilder? = null
    private var captureStart: Int = 0

    private val minusOne = (-1).toChar()
    private val isWhiteSpace: Boolean
        get() = current == ' '|| current == '\t' || current == '\n' || current == '\r'

    private val isDigit: Boolean
        get() = current in '0'..'9'

    private val isHexDigit: Boolean
        get() = (current in '0'..'9'
          || current in 'a'..'f'
          || current in 'A'..'F')

    private val isEndOfText: Boolean
        get() =
            current.toInt() == minusOne.toInt()

    /*
   * |                      bufferOffset
   *                        v
   * [values|b|c|d|e|f|g|h|i|j|k|l|fields|number|o|p|q|r|stringValue|t]        < input
   *                       [l|fields|number|o|p|q|r|stringValue|t|?|?]    < buffer
   *                          ^               ^
   *                       |  index           fill
   */

    private constructor(string: String) : this(StringReader(string),
      Math.max(MIN_BUFFER_SIZE, Math.min(DEFAULT_BUFFER_SIZE, string.length)))


    init {
        buffer = CharArray(buffersize)
        line = 1
        captureStart = -1
    }

    @Throws(IOException::class)
    private fun parse(): JsonValue {
        read()
        skipWhiteSpace()
        val result = readValue()
        skipWhiteSpace()
        if (!isEndOfText) {
            throw error("Unexpected character:${current.toInt()}")
        }
        return result
    }

    @Throws(IOException::class)
    private fun readValue(): JsonValue {
        return when (current) {
            'n' -> readNull()
            't' -> readTrue()
            'f' -> readFalse()
            '"' -> readString()
            '[' -> readArray()
            '{' -> readObject()
            '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> readNumber()
            else -> throw expected("value")
        }
    }

    @Throws(IOException::class)
    private fun readArray(): JsonArray {
        read()
        skipWhiteSpace()
        if (readChar(']')) {
            return JsonArray(List.empty<JsonValue>())
        }
        var list = List.empty<JsonValue>()
        do {
            skipWhiteSpace()
            list = list.prepend(readValue())
            skipWhiteSpace()
        } while (readChar(','))
        if (!readChar(']')) {
            throw expected("',' or ']'")
        }
        return JsonArray(list.reverse())
    }

    @Throws(IOException::class)
    private fun readObject(): JsonObject {
        read()
        skipWhiteSpace()
        if (readChar('}')) {
            return JsonObject(TreeMap.empty())
        }
        var contents = TreeMap.empty<String,JsonValue>()
        do {
            skipWhiteSpace()
            val name = readName()
            skipWhiteSpace()
            if (!readChar(':')) {
                throw expected("':'")
            }
            skipWhiteSpace()
            contents = contents.put(name, readValue())
            skipWhiteSpace()
        } while (readChar(','))
        if (!readChar('}')) {
            throw expected("',' or '}'")
        }
        return JsonObject(contents)
    }

    @Throws(IOException::class)
    private fun readName(): String {
        if (current != '"') {
            throw expected("name")
        }
        return readStringInternal()
    }

    @Throws(IOException::class)
    private fun readNull(): JsonValue {
        read()
        readRequiredChar('u')
        readRequiredChar('l')
        readRequiredChar('l')
        return JsonNull
    }

    @Throws(IOException::class)
    private fun readTrue(): JsonValue {
        read()
        readRequiredChar('r')
        readRequiredChar('u')
        readRequiredChar('e')
        return JsonBool(true)
    }

    @Throws(IOException::class)
    private fun readFalse(): JsonValue {
        read()
        readRequiredChar('a')
        readRequiredChar('l')
        readRequiredChar('s')
        readRequiredChar('e')
        return JsonBool(false)
    }

    @Throws(IOException::class)
    private fun readRequiredChar(ch: Char) {
        if (!readChar(ch)) {
            throw expected("'$ch'")
        }
    }

    @Throws(IOException::class)
    private fun readString(): JsonValue {
        return JsonString(readStringInternal())
    }

    @Throws(IOException::class)
    private fun readStringInternal(): String {
        read()
        startCapture()
        while (current != '"') {
            if (current == '\\') {
                pauseCapture()
                readEscape()
                startCapture()
            } else if (current < 0x20.toChar()) {
                throw expected("valid string character")
            } else {
                read()
            }
        }
        val string = endCapture()
        read()
        return string
    }

    @Throws(IOException::class)
    private fun readEscape() {
        read()
        when (current) {
            '"', '/', '\\' -> captureBuffer!!.append(current)
            'b' -> captureBuffer!!.append('\b')
            'n' -> captureBuffer!!.append('\n')
            //Missing /f form feed
            'r' -> captureBuffer!!.append('\r')
            't' -> captureBuffer!!.append('\t')
            'u' -> {
                val hexChars = CharArray(4)
                for (i in 0..3) {
                    read()
                    if (!isHexDigit) {
                        throw expected("hexadecimal digit")
                    }
                    hexChars[i] = current
                }
                captureBuffer!!.append(Integer.parseInt(String(hexChars), 16).toChar())
            }
            else -> throw expected("valid escape sequence")
        }
        read()
    }

    @Throws(IOException::class)
    private fun readNumber(): JsonValue {
        startCapture()
        readChar('-')
        val firstDigit = current
        if (!readDigit()) {
            throw expected("digit")
        }
        if (firstDigit != '0') {
            while (readDigit()) {
            }
        }
        readFraction()
        readExponent()
        return JsonNumber(BigDecimal(endCapture()))
    }

    @Throws(IOException::class)
    private fun readFraction(): Boolean {
        if (!readChar('.')) {
            return false
        }
        if (!readDigit()) {
            throw expected("digit")
        }
        while (readDigit()) {
        }
        return true
    }

    @Throws(IOException::class)
    private fun readExponent(): Boolean {
        if (!readChar('e') && !readChar('E')) {
            return false
        }
        if (!readChar('+')) {
            readChar('-')
        }
        if (!readDigit()) {
            throw expected("digit")
        }
        while (readDigit()) {
        }
        return true
    }

    @Throws(IOException::class)
    private fun readChar(ch: Char): Boolean {
        if (current != ch) {
            return false
        }
        read()
        return true
    }

    @Throws(IOException::class)
    private fun readDigit(): Boolean {
        if (!isDigit) {
            return false
        }
        read()
        return true
    }

    @Throws(IOException::class)
    private fun skipWhiteSpace() {
        while (isWhiteSpace) {
            read()
        }
    }

    @Throws(IOException::class)
    private fun read() {
        if (index == fill) {
            if (captureStart != -1) {
                captureBuffer!!.append(buffer, captureStart, fill - captureStart)
                captureStart = 0
            }
            bufferOffset += fill
            fill = reader.read(buffer, 0, buffer.size)
            index = 0
            if (fill == -1) {
                current = minusOne
                return
            }
        }
        if (current == '\n') {
            line++
            lineOffset = bufferOffset + index
        }
        current = buffer[index++]
    }

    private fun startCapture() {
        if (captureBuffer == null) {
            captureBuffer = StringBuilder()
        }
        captureStart = index - 1
    }

    private fun pauseCapture() {
        val end = if (current == minusOne) index else index - 1
        captureBuffer!!.append(buffer, captureStart, end - captureStart)
        captureStart = -1
    }

    private fun endCapture(): String {
        val end = if (current == minusOne) index else index - 1
        val captured: String
        if (captureBuffer!!.isNotEmpty()) {
            captureBuffer!!.append(buffer, captureStart, end - captureStart)
            captured = captureBuffer!!.toString()
            captureBuffer!!.setLength(0)
        } else {
            captured = String(buffer, captureStart, end - captureStart)
        }
        captureStart = -1
        return captured
    }

    private fun expected(expected: String): ParseFailure {
        return if (isEndOfText) {
            error("Unexpected end of input")
        } else error("Expected $expected")
    }

    private fun error(message: String): ParseFailure {
        val absIndex = bufferOffset + index
        val column = absIndex - lineOffset
        val offset = if (isEndOfText) absIndex else absIndex - 1
        return ParseFailure(message, offset, line, column - 1)
    }

    companion object {

        private val MIN_BUFFER_SIZE = 10
        private val DEFAULT_BUFFER_SIZE = 1024

        fun parse(string: String): JsonResult<JsonValue> {
            return try {
                JsonResult.success(JsonParser(string).parse())
            } catch (ioe: IOException) {
                JsonResult.fail("IOException while parsing: " + ioe.message)
            } catch (f: ParseFailure) {
                JsonResult.fail("Could not parse json: " + f.message + ": line " + f.line + ", " + f.offset + ", i" + f.i)
            }

        }

        fun parse(string: Reader): JsonResult<JsonValue> {
            return try {
                JsonResult.success(JsonParser(string).parse())
            } catch (ioe: IOException) {
                JsonResult.fail("IOException while parsing: " + ioe.message)
            } catch (f: ParseFailure) {
                JsonResult.fail("Could not parse json: " + f.message + ": line " + f.line + ", " + f.offset + ", i" + f.i)
            }

        }
    }

}
