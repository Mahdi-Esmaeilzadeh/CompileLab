package com.mahdi.compilerstudio

import com.mahdi.compilerstudio.compiler.Compiler as EngineCompiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompilerEngineTest {
    @Test fun arithmeticRunsThroughAllSixStages() {
        val result = EngineCompiler.compile("x = 7\ny = 5\nprint(x * y + 2)\n", 100_000)
        assertEquals(6, result.completedPhase)
        assertNull(result.failedPhase)
        assertEquals("37\n", result.output)
        assertTrue(result.tokens.isNotEmpty())
        assertNotNull(result.ast)
        assertTrue(result.symbols.isNotEmpty())
        assertTrue(result.tac.isNotEmpty())
        assertTrue(result.optimizedTac.isNotEmpty())
        assertTrue(result.bytecode.isNotEmpty())
    }

    @Test fun ifElseChoosesExpectedBranch() {
        val source = "x = 7\nif (x > 3) {\n print(\"yes\")\n} else {\n print(\"no\")\n}\n"
        val result = EngineCompiler.compile(source, 100_000)
        assertNull(result.failedPhase)
        assertEquals("yes\n", result.output)
    }

    @Test fun whileLoopUpdatesVariables() {
        val source = "x = 0\nwhile (x < 3) {\n print(x)\n x = x + 1\n}\n"
        val result = EngineCompiler.compile(source, 100_000)
        assertNull(result.failedPhase)
        assertEquals("0\n1\n2\n", result.output)
    }

    @Test fun nestedControlFlowExecutesInOrder() {
        val source = """x = 0
while (x < 2) {
 if (x == 0) {
  print("zero")
 } else {
  print("one")
 }
 x = x + 1
}
"""
        val result = EngineCompiler.compile(source, 100_000)
        assertNull(result.failedPhase)
        assertEquals("zero\none\n", result.output)
    }

    @Test fun stringsBooleansAndFloatsExecute() {
        val source = "name = \"Mahdi\"\nprint(\"Hello \" + name)\na = 1.5\nb = 2.0\nprint(a + b)\nprint(true and not false)\n"
        val result = EngineCompiler.compile(source, 100_000)
        assertNull(result.failedPhase)
        assertEquals("Hello Mahdi\n3.5\ntrue\n", result.output)
    }

    @Test fun constantExpressionsAreFoldedByOptimizer() {
        val result = EngineCompiler.compile("x = 2 + 3 * 4\nprint(x)\n", 100_000)
        assertNull(result.failedPhase)
        assertEquals("14\n", result.output)
        assertTrue(result.tac.size > result.optimizedTac.size)
        assertTrue(result.optimizedTac.any { it.toString().contains("14") })
    }

    @Test fun parserFailureReportsItsPipelineStage() {
        val result = EngineCompiler.compile("x =\n", 100_000)
        assertEquals(1, result.completedPhase)
        assertEquals(2, result.failedPhase)
        assertTrue(result.diagnostics.any { it.phase == "Parser" })
        assertFalse(result.diagnostics.isEmpty())
    }

    @Test fun undefinedVariableIsRejectedBySemanticAnalysis() {
        val result = EngineCompiler.compile("print(missing)\n", 100_000)
        assertEquals(2, result.completedPhase)
        assertEquals(3, result.failedPhase)
        assertTrue(result.diagnostics.any { it.phase == "Semantic" })
    }

    @Test fun vmStopsProgramsThatExceedExecutionLimit() {
        val source = "x = 0\nwhile (true) {\n x = x + 1\n}\n"
        val result = EngineCompiler.compile(source, 10)
        assertEquals(5, result.completedPhase)
        assertEquals(6, result.failedPhase)
        assertTrue(result.diagnostics.any { it.message.contains("step limit") })
    }

    @Test fun moduloAndNestedIfInsideWhileExecute() {
        val source = """number = 1
while (number <= 5) {
 if (number % 2 == 0) {
  print("even")
 } else {
  print("odd")
 }
 number = number + 1
}
"""
        val result = EngineCompiler.compile(source, 100_000)
        assertNull(result.failedPhase)
        assertEquals("odd\neven\nodd\neven\nodd\n", result.output)
    }

    @Test fun arithmeticOperatorPrecedenceIsRespected() {
        val result = EngineCompiler.compile("print(1 + 2 * 3)\n", 100_000)
        assertNull(result.failedPhase)
        assertEquals("7\n", result.output)
    }

    @Test fun commentsAndDivisionAreAccepted() {
        val result = EngineCompiler.compile("# test comment\nprint(8 / 2)\n", 100_000)
        assertNull(result.failedPhase)
        assertEquals("4\n", result.output)
    }
}
