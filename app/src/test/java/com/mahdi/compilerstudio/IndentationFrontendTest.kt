package com.mahdi.compilerstudio

import com.mahdi.compilerstudio.compiler.Compiler as EngineCompiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IndentationFrontendTest {
    @Test fun translatesIfElseAndRunsIt() {
        val source = "score = 82\nif score >= 60:\n    print(\"Passed\")\nelse:\n    print(\"Try again\")\n"
        val translated = IndentationFrontend.translate(source)
        assertTrue(translated.usedIndentation)
        val result = EngineCompiler.compile(translated.source, 100_000)
        assertEquals(null, result.failedPhase)
        assertEquals("Passed\n", result.output)
    }

    @Test fun translatesWhileAndRunsIt() {
        val source = "n = 0\nwhile n < 3:\n    print(n)\n    n = n + 1\n"
        val result = EngineCompiler.compile(IndentationFrontend.translate(source).source, 100_000)
        assertEquals(null, result.failedPhase)
        assertEquals("0\n1\n2\n", result.output)
    }

    @Test fun translatesNestedElifBranches() {
        val source = "n = 2\nif n == 1:\n    print(\"one\")\nelif n == 2:\n    print(\"two\")\nelse:\n    print(\"other\")\n"
        val result = EngineCompiler.compile(IndentationFrontend.translate(source).source, 100_000)
        assertEquals(null, result.failedPhase)
        assertEquals("two\n", result.output)
    }

    @Test fun rejectsTabsAndSpacesMixedAcrossLines() {
        val source = "x = 1\nif x > 0:\n    print(1)\n\tprint(2)\n"
        val error = try { IndentationFrontend.translate(source); null } catch (e: IndentationFrontend.FrontendException) { e }
        assertTrue(error != null)
        assertEquals(4, error!!.sourceLine)
    }

    @Test fun acceptsConsistentTabIndentation() {
        val source = "x = 1\nif x > 0:\n\tprint(1)\n\tprint(2)\n"
        val result = EngineCompiler.compile(IndentationFrontend.translate(source).source, 100_000)
        assertEquals(null, result.failedPhase)
        assertEquals("1\n2\n", result.output)
    }

    @Test fun translatesTrueAndFalseLiterals() {
        val source = "flag = True\nif flag:\n    print(False)\n"
        val result = EngineCompiler.compile(IndentationFrontend.translate(source).source, 100_000)
        assertEquals(null, result.failedPhase)
        assertEquals("false\n", result.output)
    }

    @Test fun reportsUnexpectedIndentation() {
        try {
            IndentationFrontend.translate("x = 1\n    print(x)\n")
            throw AssertionError("Expected indentation error")
        } catch (error: IndentationFrontend.FrontendException) {
            assertTrue(error.message!!.contains("indentation"))
            assertEquals(2, error.sourceLine)
        }
    }

    @Test fun rejectsMissingBlockBody() {
        try {
            IndentationFrontend.translate("if True:\n")
            throw AssertionError("Expected missing body error")
        } catch (error: IndentationFrontend.FrontendException) {
            assertTrue(error.message!!.contains("indented body"))
        }
    }


    @Test fun nestedElseBindsToItsNearestIf() {
        val source = "a = True\nb = False\nif a:\n    if b:\n        print(\"inner true\")\n    else:\n        print(\"inner false\")\nelse:\n    print(\"outer false\")\n"
        val result = EngineCompiler.compile(IndentationFrontend.translate(source).source, 100_000)
        assertEquals(null, result.failedPhase)
        assertEquals("inner false\n", result.output)
    }


    @Test fun translatesSingleArgumentRangeLoop() {
        val source = "for i in range(4):\n    print(i)\n"
        val result = EngineCompiler.compile(IndentationFrontend.translate(source).source, 100_000)
        assertEquals(null, result.failedPhase)
        assertEquals("0\n1\n2\n3\n", result.output)
    }

    @Test fun translatesRangeWithStartStopAndStep() {
        val source = "for i in range(2, 7, 2):\n    print(i)\n"
        val result = EngineCompiler.compile(IndentationFrontend.translate(source).source, 100_000)
        assertEquals(null, result.failedPhase)
        assertEquals("2\n4\n6\n", result.output)
    }

    @Test fun translatesDescendingRangeLoop() {
        val source = "for i in range(5, 0, -2):\n    print(i)\n"
        val result = EngineCompiler.compile(IndentationFrontend.translate(source).source, 100_000)
        assertEquals(null, result.failedPhase)
        assertEquals("5\n3\n1\n", result.output)
    }

    @Test fun translatesNestedRangeLoops() {
        val source = "for i in range(2):\n    for j in range(2):\n        print(i * 10 + j)\n"
        val result = EngineCompiler.compile(IndentationFrontend.translate(source).source, 100_000)
        assertEquals(null, result.failedPhase)
        assertEquals("0\n1\n10\n11\n", result.output)
    }

    @Test fun rejectsZeroRangeStepAndNonLiteralRange() {
        listOf("for i in range(1, 5, 0):\n    print(i)\n", "for i in range(limit):\n    print(i)\n").forEach { source ->
            try {
                IndentationFrontend.translate(source)
                throw AssertionError("Expected range argument error")
            } catch (error: IndentationFrontend.FrontendException) {
                assertTrue(error.message!!.contains("range()"))
            }
        }
    }

    @Test fun keepsLegacyBraceSyntaxWorking() {
        val source = "x = 1\nif (x == 1) {\n print(\"ok\")\n}\n"
        val translated = IndentationFrontend.translate(source)
        assertFalse(translated.usedIndentation)
        assertEquals(source, translated.source)
        val result = EngineCompiler.compile(translated.source, 100_000)
        assertEquals("ok\n", result.output)
    }
}
