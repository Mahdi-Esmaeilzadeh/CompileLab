package com.mahdi.compilerstudio

/**
 * Converts a deliberately small Python-like indentation syntax into the bundled
 * educational engine's brace-based syntax. This is a frontend adapter, not CPython.
 */
object IndentationFrontend {
    data class Translation(val source: String, val usedIndentation: Boolean, val lineMap: List<Int> = emptyList())

    class FrontendException(message: String, val sourceLine: Int) : IllegalArgumentException("Line $sourceLine: $message")

    private enum class BlockKind { IF, ELSE, WHILE }
    private data class Block(
        val headerIndent: Int,
        val kind: BlockKind,
        var bodyIndent: Int? = null,
        val loopIncrement: String? = null,
        val headerLine: Int
    )

    private val ifPattern = Regex("^if\\s+(.+):$")
    private val elifPattern = Regex("^elif\\s+(.+):$")
    private val whilePattern = Regex("^while\\s+(.+):$")
    private val headerNeedingColon = Regex("^((if|elif|while|for)\\b.*|else)$")
    private val forRangePattern = Regex("^for\\s+([A-Za-z][A-Za-z0-9_]*)\\s+in\\s+range\\((.*)\\):$")

    fun translate(input: String, inputLineMap: List<Int> = emptyList()): Translation {
        // Preserve compatibility with the original brace-based educational syntax.
        val hasBraceHeader = input.lineSequence().any {
            Regex("^\\s*(if|while)\\s*\\(.*\\)\\s*\\{\\s*$").matches(it)
        }
        if (hasBraceHeader) return Translation(input, false, linesMap(input, inputLineMap))

        val blocks = mutableListOf<Block>()
        val output = StringBuilder()
        val outputLineMap = mutableListOf<Int>()
        val lines = input.lines()
        // Tabs and spaces must not be mixed anywhere in one program's indentation.
        var fileIndentChar: Char? = null
        lines.forEachIndexed { index, line ->
            val t = line.trim()
            if (t.isEmpty() || t.startsWith("#")) return@forEachIndexed
            val first = line.firstOrNull()
            if (first == ' ' || first == '\t') {
                val seen = fileIndentChar
                if (seen == null) fileIndentChar = first
                else if (seen != first) throw FrontendException("Do not mix tabs and spaces for indentation", index + 1)
            }
        }
        fun mappedLine(line: Int): Int = inputLineMap.getOrNull(line - 1) ?: line
        fun emit(text: String, sourceLine: Int) {
            output.appendLine(text)
            outputLineMap += mappedLine(sourceLine)
        }

        lines.forEachIndexed { index, original ->
            val lineNumber = index + 1
            val trimmed = original.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                emit(original, lineNumber)
                return@forEachIndexed
            }

            val indent = indentationWidth(original, lineNumber)
            if (headerNeedingColon.matches(trimmed) && !trimmed.contains(':')) {
                throw FrontendException("Missing ':' at the end of the line", lineNumber)
            }
            val isElseLike = trimmed == "else:" || elifPattern.matches(trimmed)

            // Close any nested suites before resolving an else/elif belonging to an outer if.
            if (isElseLike) {
                while (blocks.isNotEmpty() && indent < blocks.last().headerIndent) {
                    closeBlock(blocks.removeAt(blocks.lastIndex), ::emit, lineNumber)
                }
            }

            if (isElseLike && blocks.lastOrNull()?.headerIndent == indent && blocks.last().kind == BlockKind.IF) {
                val current = blocks.last()
                if (current.bodyIndent == null) {
                    throw FrontendException("Expected an indented body before this branch", lineNumber)
                }
                if (trimmed == "else:") {
                    emit("} else {", lineNumber)
                    blocks[blocks.lastIndex] = Block(indent, BlockKind.ELSE, headerLine = lineNumber)
                } else {
                    val condition = elifPattern.matchEntire(trimmed)!!.groupValues[1]
                    emit("} else {", lineNumber)
                    emit("if (${normalizeExpression(condition)}) {", lineNumber)
                    blocks[blocks.lastIndex] = Block(indent, BlockKind.ELSE, headerLine = lineNumber)
                    blocks += Block(indent, BlockKind.IF, headerLine = lineNumber)
                }
                return@forEachIndexed
            }

            while (blocks.isNotEmpty() && indent <= blocks.last().headerIndent) {
                closeBlock(blocks.removeAt(blocks.lastIndex), ::emit, lineNumber)
            }

            val parent = blocks.lastOrNull()
            if (parent == null) {
                if (indent != 0) throw FrontendException("Unexpected indentation at top level", lineNumber)
            } else {
                val expected = parent.bodyIndent
                if (expected == null) {
                    if (indent <= parent.headerIndent) {
                        throw FrontendException("Expected an indented block", lineNumber)
                    }
                    parent.bodyIndent = indent
                    // An elif is lowered to an if nested inside a synthetic else;
                    // both suites begin at the same source indentation.
                    for (i in blocks.lastIndex - 1 downTo 0) {
                        val sibling = blocks[i]
                        if (sibling.headerIndent == parent.headerIndent && sibling.bodyIndent == null) {
                            sibling.bodyIndent = indent
                        } else if (sibling.headerIndent != parent.headerIndent) {
                            break
                        }
                    }
                } else if (indent != expected) {
                    val reason = if (indent > expected) "Unexpected extra indentation" else "Inconsistent indentation"
                    throw FrontendException(reason, lineNumber)
                }
            }

            when {
                forRangePattern.matches(trimmed) -> {
                    val match = forRangePattern.matchEntire(trimmed)!!
                    val variable = match.groupValues[1]
                    val args = match.groupValues[2].split(',').map { it.trim() }
                    val values = args.map { it.toIntOrNull() }
                    if (args.size !in 1..3 || values.any { it == null }) {
                        throw FrontendException("range() currently accepts one to three 32-bit integer literals", lineNumber)
                    }
                    val start = if (args.size == 1) 0 else values[0]!!
                    val stop = if (args.size == 1) values[0]!! else values[1]!!
                    val step = if (args.size == 3) values[2]!! else 1
                    if (step == 0) throw FrontendException("range() step cannot be zero", lineNumber)
                    val comparison = if (step > 0) "<" else ">"
                    emit("$variable = $start", lineNumber)
                    emit("while ($variable $comparison $stop) {", lineNumber)
                    blocks += Block(indent, BlockKind.WHILE, loopIncrement = "$variable = $variable + $step", headerLine = lineNumber)
                }
                ifPattern.matches(trimmed) -> {
                    val condition = ifPattern.matchEntire(trimmed)!!.groupValues[1]
                    emit("if (${normalizeExpression(condition)}) {", lineNumber)
                    blocks += Block(indent, BlockKind.IF, headerLine = lineNumber)
                }
                whilePattern.matches(trimmed) -> {
                    val condition = whilePattern.matchEntire(trimmed)!!.groupValues[1]
                    emit("while (${normalizeExpression(condition)}) {", lineNumber)
                    blocks += Block(indent, BlockKind.WHILE, headerLine = lineNumber)
                }
                trimmed == "else:" || trimmed.startsWith("elif ") -> {
                    throw FrontendException("Branch has no matching if at this indentation level", lineNumber)
                }
                trimmed.endsWith(":") && (trimmed.startsWith("if ") || trimmed.startsWith("while ") || trimmed.startsWith("for ")) -> {
                    throw FrontendException("Malformed or unsupported control-flow header", lineNumber)
                }
                trimmed.startsWith("for ") -> {
                    throw FrontendException("Only for-variable in range(integer, ...) loops are supported", lineNumber)
                }
                else -> emit(normalizeStatement(original), lineNumber)
            }
        }

        while (blocks.isNotEmpty()) {
            closeBlock(blocks.removeAt(blocks.lastIndex), ::emit, lines.size.coerceAtLeast(1))
        }
        return Translation(output.toString(), true, outputLineMap)
    }

    private fun closeBlock(block: Block, emit: (String, Int) -> Unit, lineNumber: Int) {
        if (block.bodyIndent == null) {
            throw FrontendException("Expected an indented body after the block header", lineNumber)
        }
        block.loopIncrement?.let { emit("${" ".repeat(block.headerIndent)}$it", block.headerLine) }
        emit("}", block.headerLine)
    }

    private fun linesMap(input: String, inputLineMap: List<Int>): List<Int> = input.lines().indices.map { index ->
        inputLineMap.getOrNull(index) ?: index + 1
    }

    private fun indentationWidth(line: String, lineNumber: Int): Int {
        var width = 0
        var sawSpace = false
        var sawTab = false
        for (ch in line) {
            when (ch) {
                ' ' -> { width++; sawSpace = true }
                '\t' -> { width = ((width / 4) + 1) * 4; sawTab = true }
                else -> break
            }
        }
        if (sawSpace && sawTab) throw FrontendException("Do not mix tabs and spaces for indentation", lineNumber)
        return width
    }

    private fun normalizeExpression(expression: String): String = expression
        .replace(Regex("\\bTrue\\b"), "true")
        .replace(Regex("\\bFalse\\b"), "false")

    private fun normalizeStatement(original: String): String {
        val leading = original.takeWhile { it == ' ' || it == '\t' }
        val body = original.drop(leading.length)
        return leading + normalizeExpression(body)
    }
}
