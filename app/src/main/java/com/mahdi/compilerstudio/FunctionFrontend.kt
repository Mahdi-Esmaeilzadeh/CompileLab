package com.mahdi.compilerstudio

/**
 * Small, explicit function-expansion frontend for the educational VM.
 * Supports top-level `def name(parameters):` declarations and statement-position
 * calls. Functions are expanded at compile time; this is not runtime function
 * dispatch and recursion is intentionally rejected.
 */
object FunctionFrontend {
    data class Translation(val source: String, val usedFunctions: Boolean, val lineMap: List<Int> = emptyList())

    class FrontendException(message: String, val sourceLine: Int) :
        IllegalArgumentException("Line $sourceLine: $message")

    private data class FunctionDef(
        val name: String,
        val parameters: List<String>,
        val body: List<SourceLine>,
        val definitionLine: Int
    )

    private data class SourceLine(val text: String, val line: Int, val indent: Int)
    private enum class CallKind { STATEMENT, ASSIGNMENT, PRINT }
    private data class CallSite(
        val kind: CallKind,
        val functionName: String,
        val arguments: String,
        val target: String? = null,
        val suffix: String = ""
    )

    private val definitionPattern = Regex("^def\\s+([A-Za-z][A-Za-z0-9_]*)\\s*\\((.*)\\)\\s*:$")
    private val assignmentPattern = Regex("^\\s*([A-Za-z][A-Za-z0-9_]*)\\s*=")

    fun translate(input: String): Translation {
        val lines = input.lines()
        val definitions = linkedMapOf<String, FunctionDef>()
        val definitionLineIndexes = mutableSetOf<Int>()
        var i = 0

        while (i < lines.size) {
            val raw = lines[i]
            val trimmed = raw.trim()
            val indent = leadingWidth(raw)
            val match = definitionPattern.matchEntire(trimmed)
            if (match == null) {
                if (trimmed.startsWith("def ")) {
                    throw FrontendException("Malformed function definition", i + 1)
                }
                i++
                continue
            }
            if (indent != 0) throw FrontendException("Function definitions must be at top level", i + 1)
            val name = match.groupValues[1]
            if (definitions.containsKey(name)) throw FrontendException("Function '$name' is already defined", i + 1)
            val params = parseParameters(match.groupValues[2], i + 1)
            val defLine = i + 1
            definitionLineIndexes += i
            i++
            val body = mutableListOf<SourceLine>()
            var bodyIndent: Int? = null
            while (i < lines.size) {
                val bodyRaw = lines[i]
                if (bodyRaw.isBlank() || bodyRaw.trimStart().startsWith("#")) {
                    if (bodyIndent != null) {
                        body += SourceLine(bodyRaw, i + 1, leadingWidth(bodyRaw))
                        definitionLineIndexes += i
                    }
                    i++
                    continue
                }
                val currentIndent = leadingWidth(bodyRaw)
                if (currentIndent == 0) break
                if (bodyIndent == null) bodyIndent = currentIndent
                if (currentIndent < bodyIndent) break
                body += SourceLine(bodyRaw, i + 1, currentIndent)
                definitionLineIndexes += i
                i++
            }
            if (bodyIndent == null || body.none { it.text.isNotBlank() }) {
                throw FrontendException("Function '$name' must have an indented body", defLine)
            }
            definitions[name] = FunctionDef(name, params, body, defLine)
        }

        if (definitions.isEmpty()) return Translation(input, false, lines.indices.map { it + 1 })

        val output = StringBuilder()
        val outputLineMap = mutableListOf<Int>()
        fun emit(text: String, sourceLine: Int) {
            output.appendLine(text)
            outputLineMap += sourceLine
        }
        var generatedId = 0
        lines.forEachIndexed { index, raw ->
            if (index in definitionLineIndexes) return@forEachIndexed
            val trimmed = raw.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                emit(raw, index + 1)
                return@forEachIndexed
            }
            val indent = leadingWidth(raw)
            val callSite = parseCallSite(trimmed)
            val functionName = callSite?.functionName
            if (callSite == null || functionName == null || functionName !in definitions) {
                if (trimmed.startsWith("return")) {
                    throw FrontendException("return is only supported as the final top-level statement of a function", index + 1)
                }
                emit(raw, index + 1)
                return@forEachIndexed
            }
            val def = definitions.getValue(functionName)
            val args = splitArguments(callSite.arguments, index + 1)
            if (args.size != def.parameters.size) {
                throw FrontendException("Function '$functionName' expects ${def.parameters.size} arguments, got ${args.size}", index + 1)
            }
            val nestedCall = def.body.firstOrNull { line ->
                definitions.keys.any { candidate -> Regex("\\b${Regex.escape(candidate)}\\s*\\(").containsMatchIn(line.text) }
            }
            if (nestedCall != null) {
                val message = if (nestedCall.text.contains("$functionName(")) "Recursive function calls are not supported" else "Calling another function from a function is not supported yet"
                throw FrontendException(message, nestedCall.line)
            }

            generatedId++
            val bodyBase = def.body.filter { it.text.isNotBlank() }.minOf { it.indent }
            // Materialize each argument in a fresh per-expansion slot. This keeps
            // parameter rebinding local and avoids textual substitution of expressions.
            val parameterMap = def.parameters.associateWith { "__cs_fn_${generatedId}_param_$it" }
            val localNames = linkedSetOf<String>()
            def.body.forEach { line ->
                assignmentPattern.find(line.text)?.groupValues?.get(1)?.let { if (it !in def.parameters) localNames += it }
                Regex("^\\s*for\\s+([A-Za-z][A-Za-z0-9_]*)\\s+in\\s+").find(line.text)
                    ?.groupValues?.get(1)?.let { if (it !in def.parameters) localNames += it }
            }
            val renamedLocals = localNames.associateWith { "__cs_fn_${generatedId}_$it" }
            val resultName = "__cs_fn_${generatedId}_result"
            val bareReturn = def.body.firstOrNull { it.text.trim() == "return" }
            if (bareReturn != null) throw FrontendException("return must include a value expression", bareReturn.line)
            val returnLines = def.body.filter { it.text.trimStart().startsWith("return ") }
            if (returnLines.size > 1 || (returnLines.isNotEmpty() && returnLines.single() != def.body.last { it.text.isNotBlank() })) {
                throw FrontendException("Only one final return statement is supported in function '$functionName'", returnLines.firstOrNull()?.line ?: def.definitionLine)
            }
            val returnLine = returnLines.singleOrNull()
            if (returnLine != null && returnLine.indent != bodyBase) {
                throw FrontendException("return must be unconditional and aligned with the function body", returnLine.line)
            }
            if (callSite.kind == CallKind.PRINT && returnLine == null) {
                throw FrontendException("Function '$functionName' does not return a value", index + 1)
            }
            def.parameters.forEachIndexed { parameterIndex, parameter ->
                emit("${" ".repeat(indent)}${parameterMap.getValue(parameter)} = ${args[parameterIndex]}", index + 1)
            }
            if (callSite.kind == CallKind.ASSIGNMENT && returnLine == null) {
                throw FrontendException("Function '$functionName' does not return a value", index + 1)
            }

            def.body.forEach { bodyLine ->
                if (bodyLine.text.isBlank() || bodyLine.text.trimStart().startsWith("#")) return@forEach
                if (bodyLine == returnLine) {
                    val expression = bodyLine.text.trim().removePrefix("return").trim()
                    val expanded = substitute(expression, parameterMap, renamedLocals)
                    emit("${" ".repeat(indent)}$resultName = $expanded", bodyLine.line)
                } else {
                    val relativeIndent = (bodyLine.indent - bodyBase).coerceAtLeast(0)
                    val bodyText = bodyLine.text.trimStart()
                    val expanded = substitute(bodyText, parameterMap, renamedLocals)
                    emit("${" ".repeat(indent + relativeIndent)}$expanded", bodyLine.line)
                }
            }
            when (callSite.kind) {
                CallKind.ASSIGNMENT -> emit("${" ".repeat(indent)}${callSite.target} = $resultName${callSite.suffix}", index + 1)
                CallKind.PRINT -> emit("${" ".repeat(indent)}print($resultName)", index + 1)
                CallKind.STATEMENT -> Unit
            }
        }
        return Translation(output.toString(), true, outputLineMap)
    }

    private fun parseCallSite(statement: String): CallSite? {
        val printPrefix = Regex("^print\\(\\s*([A-Za-z][A-Za-z0-9_]*)\\s*\\(").find(statement)
        if (printPrefix != null) {
            val open = statement.indexOf('(', printPrefix.range.first + printPrefix.value.length - 1)
            val close = matchingParen(statement, open) ?: return null
            val tail = statement.substring(close + 1).trim()
            if (tail != ")") return null
            return CallSite(CallKind.PRINT, printPrefix.groupValues[1], statement.substring(open + 1, close))
        }

        val assignmentPrefix = Regex("^([A-Za-z][A-Za-z0-9_]*)\\s*=\\s*([A-Za-z][A-Za-z0-9_]*)\\s*\\(").find(statement)
        if (assignmentPrefix != null) {
            val open = statement.indexOf('(', assignmentPrefix.range.last)
            val close = matchingParen(statement, open) ?: return null
            return CallSite(
                CallKind.ASSIGNMENT,
                assignmentPrefix.groupValues[2],
                statement.substring(open + 1, close),
                target = assignmentPrefix.groupValues[1],
                suffix = statement.substring(close + 1)
            )
        }

        val statementPrefix = Regex("^([A-Za-z][A-Za-z0-9_]*)\\s*\\(").find(statement) ?: return null
        val statementOpen = statement.indexOf('(', statementPrefix.range.last)
        val statementClose = matchingParen(statement, statementOpen) ?: return null
        if (statement.substring(statementClose + 1).isNotBlank()) return null
        return CallSite(CallKind.STATEMENT, statementPrefix.groupValues[1], statement.substring(statementOpen + 1, statementClose))
    }

    private fun matchingParen(text: String, openIndex: Int): Int? {
        if (openIndex !in text.indices || text[openIndex] != '(') return null
        var depth = 0
        var quote: Char? = null
        var escaped = false
        for (index in openIndex until text.length) {
            val ch = text[index]
            if (quote != null) {
                if (escaped) escaped = false
                else if (ch == '\\') escaped = true
                else if (ch == quote) quote = null
            } else when (ch) {
                '\'', '"' -> quote = ch
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }

    private fun parseParameters(text: String, line: Int): List<String> {
        if (text.isBlank()) return emptyList()
        val params = splitArguments(text, line)
        if (params.any { !Regex("[A-Za-z][A-Za-z0-9_]*").matches(it) }) {
            throw FrontendException("Function parameters must be simple identifiers", line)
        }
        if (params.distinct().size != params.size) throw FrontendException("Duplicate function parameter", line)
        return params
    }

    private fun splitArguments(text: String, line: Int): List<String> {
        if (text.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        var depth = 0
        var quote: Char? = null
        var escaped = false
        var start = 0
        text.forEachIndexed { index, ch ->
            if (quote != null) {
                if (escaped) escaped = false
                else if (ch == '\\') escaped = true
                else if (ch == quote) quote = null
            } else when (ch) {
                '\'', '"' -> quote = ch
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                ',' -> if (depth == 0) {
                    result += text.substring(start, index).trim()
                    start = index + 1
                }
            }
            if (depth < 0) throw FrontendException("Unbalanced function-call arguments", line)
        }
        if (quote != null || depth != 0) throw FrontendException("Unbalanced function-call arguments", line)
        result += text.substring(start).trim()
        if (result.any { it.isEmpty() }) throw FrontendException("Empty function argument", line)
        return result
    }

    private fun substitute(
        source: String,
        parameters: Map<String, String>,
        locals: Map<String, String>
    ): String {
        val replacements = parameters + locals
        val output = StringBuilder()
        var index = 0
        var quote: Char? = null
        var escaped = false
        while (index < source.length) {
            val ch = source[index]
            if (quote != null) {
                output.append(ch)
                if (escaped) escaped = false
                else if (ch == '\\') escaped = true
                else if (ch == quote) quote = null
                index++
            } else if (ch == '\'' || ch == '\"') {
                quote = ch
                output.append(ch)
                index++
            } else if (ch.isLetter() || ch == '_') {
                val start = index
                index++
                while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) index++
                val identifier = source.substring(start, index)
                output.append(replacements[identifier] ?: identifier)
            } else {
                output.append(ch)
                index++
            }
        }
        return output.toString()
    }

    private fun leadingWidth(line: String): Int = line.takeWhile { it == ' ' || it == '\t' }
        .fold(0) { width, ch -> if (ch == '\t') ((width / 4) + 1) * 4 else width + 1 }
}
