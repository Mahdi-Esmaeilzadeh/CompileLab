package com.mahdi.compilerstudio

import com.mahdi.compilerstudio.compiler.CompilationResult
import com.mahdi.compilerstudio.compiler.Compiler as EngineCompiler

/**
 * Runs the complete chain used by the app:
 * FunctionFrontend -> IndentationFrontend -> engine (lexer .. VM).
 * Pure Kotlin (no Android types) so it can be unit tested on the JVM.
 */
object Pipeline {
    /** A problem found by one of the two frontends, with the line in the user's ORIGINAL source. */
    data class FrontendIssue(val line: Int, val message: String)

    data class Outcome(
        /** The text finally handed to the engine (braces, semicolons...), when the frontends succeeded. */
        val loweredSource: String?,
        /** lineMap[i] = original source line for line i+1 of [loweredSource]. */
        val lineMap: List<Int>,
        val result: CompilationResult?,
        val frontendIssue: FrontendIssue?
    )

    /** Maps a line of the generated (lowered) source back to the user's original line. */
    fun originalLine(lineMap: List<Int>, generatedLine: Int): Int =
        lineMap.getOrNull(generatedLine - 1) ?: lineMap.lastOrNull() ?: generatedLine

    fun run(source: String, maxSteps: Int = 100_000): Outcome {
        var functionLineMap: List<Int> = emptyList()
        return try {
            val expanded = FunctionFrontend.translate(source)
            functionLineMap = expanded.lineMap
            val lowered = IndentationFrontend.translate(expanded.source, expanded.lineMap)
            val result = EngineCompiler.compile(lowered.source, maxSteps)
            Outcome(lowered.source, lowered.lineMap, result, null)
        } catch (e: FunctionFrontend.FrontendException) {
            Outcome(null, emptyList(), null, FrontendIssue(e.sourceLine, e.message.orEmpty().removePrefix("Line ${e.sourceLine}: ")))
        } catch (e: IndentationFrontend.FrontendException) {
            val original = functionLineMap.getOrNull(e.sourceLine - 1) ?: e.sourceLine
            Outcome(null, emptyList(), null, FrontendIssue(original, e.message.orEmpty().removePrefix("Line ${e.sourceLine}: ")))
        } catch (t: Throwable) {
            Outcome(null, emptyList(), null, FrontendIssue(0, t.message ?: "Unexpected compiler error"))
        }
    }
}
