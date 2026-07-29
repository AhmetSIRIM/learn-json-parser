package io.github.ahmetsirim.json

/**
 * 1-based line and column, matching editor status bars so a reported
 * position can be jumped to directly. Zero-based offsets are the
 * machine-friendly alternative; error messages are for humans.
 */
data class TextPosition(val line: Int, val column: Int) {
    override fun toString(): String = "line $line, column $column"
}
