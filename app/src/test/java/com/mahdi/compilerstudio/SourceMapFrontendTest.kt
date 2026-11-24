package com.mahdi.compilerstudio

import com.mahdi.compilerstudio.compiler.Compiler as EngineCompiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceMapFrontendTest {
    @Test fun functionExpansionTracksOriginalBodyAndCallsiteLines() {
        val source = """# header

def add(a, b):
    total = a + b
    return total

answer = add(2, 3)
print(answer)
"""
        val translated = FunctionFrontend.translate(source)
        val generatedLines = translated.source.lines()
        val bodyLine = generatedLines.indexOfFirst { it.contains(" = __cs_fn_") && it.contains(" + ") } + 1
        val callLine = generatedLines.indexOfFirst { it.startsWith("answer =") } + 1
        assertEquals(4, translated.lineMap[bodyLine - 1])
        assertEquals(7, translated.lineMap[callLine - 1])
    }

    @Test fun indentationExpansionMapsSyntheticBracesToTheirHeader() {
        val source = "x = 1\nif x == 1:\n    print(x)\n"
        val translated = IndentationFrontend.translate(source)
        val closingBraceLine = translated.source.lines().indexOf("}") + 1
        assertEquals(2, translated.lineMap[closingBraceLine - 1])
        assertEquals(2, translated.lineMap[1])
    }

    @Test fun composedFunctionAndIndentationMapsPointDiagnosticsToUserSource() {
        val source = """# demo

def calculate(x):
    temp = x + missing
    return temp

answer = calculate(3)
print(answer)
"""
        val functions = FunctionFrontend.translate(source)
        val lowered = IndentationFrontend.translate(functions.source, functions.lineMap)
        val result = EngineCompiler.compile(lowered.source, 100_000)
        assertEquals(3, result.failedPhase)
        val diagnosticLine = result.diagnostics.first().pos!!.line
        assertEquals(4, lowered.lineMap[diagnosticLine - 1])
    }

    @Test fun mapsGeneratedFunctionParameterSetupToCallsite() {
        val source = "def echo(value):\n    print(value)\n\necho(9)\n"
        val translated = FunctionFrontend.translate(source)
        val generatedIndex = translated.source.lines().indexOfFirst { it.contains("_param_value = 9") }
        assertTrue(generatedIndex >= 0)
        assertEquals(4, translated.lineMap[generatedIndex])
    }
}
