package com.mahdi.compilerstudio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureLayerTest {
    private fun run(code: String) = Pipeline.run(code)
    private fun hasPersian(s: String) = s.any { it in '\u0600'..'\u06FF' }

    // ---------- Pipeline + examples ----------

    @Test fun everyGoodExampleCompilesAndRuns() {
        for (ex in Examples.good) {
            val o = run(ex.code)
            assertNull("${ex.nameEn}: frontend issue ${o.frontendIssue}", o.frontendIssue)
            assertNotNull(ex.nameEn, o.result)
            assertNull("${ex.nameEn}: ${o.result?.diagnostics}", o.result!!.failedPhase)
            assertEquals(6, o.result!!.completedPhase)
        }
    }

    @Test fun exampleOutputsAreCorrect() {
        fun out(name: String) = run(Examples.good.first { it.nameEn == name }.code).result!!.output
        assertEquals("37\n", out("Arithmetic"))
        assertEquals("Passed\n", out("If / Else"))
        assertEquals("0\n1\n2\n3\n", out("While loop"))
        assertEquals("15\n", out("For / range"))
        assertEquals("first\neven\nodd\neven\nodd\n", out("Nested + elif"))
        assertEquals("41\n19\n", out("Functions"))
        assertEquals("120\n", out("Factorial"))
        assertEquals("Hello Sara\nسلام Sara\n", out("Text"))
        val fizz = out("FizzBuzz").trim().split("\n")
        assertEquals(15, fizz.size)
        assertEquals("FizzBuzz", fizz[14]); assertEquals("Fizz", fizz[2]); assertEquals("Buzz", fizz[4]); assertEquals("1", fizz[0])
    }

    @Test fun everyMistakeExampleReportsAFriendlyError() {
        for (ex in Examples.mistakes) {
            val o = run(ex.code)
            val errors = ErrorExplainer.forOutcome(o, ex.code, Lang.EN)
            assertTrue("${ex.nameEn} should fail", errors.isNotEmpty())
            val e = errors.first()
            assertTrue(ex.nameEn, e.title.isNotBlank() && e.explanation.isNotBlank() && e.tip.isNotBlank())
            assertFalse("raw newline leaked: ${e.title}", e.title.contains('\n') || e.explanation.contains('\n'))
        }
    }

    @Test fun mistakesAreReportedByTheExpectedStage() {
        fun stage(name: String) = ErrorExplainer.forOutcome(run(Examples.mistakes.first { it.nameEn == name }.code), Examples.mistakes.first { it.nameEn == name }.code, Lang.EN).first()
        assertEquals(2, stage("Missing )").stage)
        assertEquals(0, stage("Missing :").stage)
        assertEquals(3, stage("Unknown variable").stage)
        assertEquals(3, stage("Wrong type").stage)
        assertEquals(1, stage("Bad character").stage)
        assertEquals(6, stage("Endless loop").stage)
    }

    @Test fun missingColonPointsToTheHeaderLine() {
        val code = "x = 3\nif x > 1\n    print(1)\n"
        val e = ErrorExplainer.forOutcome(run(code), code, Lang.EN).first()
        assertEquals(2, e.line)
        assertEquals("if x > 1", e.sourceLine)
        assertTrue(e.title.contains("colon", ignoreCase = true))
    }

    @Test fun variablesNamedLikeKeywordsAreNotTreatedAsMissingColon() {
        val code = "iffy = 3\nformat = 4\nelse_x = 5\nprint(iffy + format + else_x)\n"
        assertEquals("12\n", run(code).result!!.output)
    }

    @Test fun functionErrorsAreMappedToOriginalLines() {
        val code = "def f(a):\n    return a\nprint(f(1,2))\n"
        val e = ErrorExplainer.forOutcome(run(code), code, Lang.EN).first()
        assertEquals(3, e.line)
        assertTrue(e.title.contains("arguments"))
    }

    @Test fun errorLineIsMappedThroughBothFrontends() {
        val code = "def f(a):\n    return a\ny = f(1)\nprint(zzz)\n"
        val e = ErrorExplainer.forOutcome(run(code), code, Lang.EN).first()
        assertEquals(4, e.line)
        assertEquals("print(zzz)", e.sourceLine)
        assertEquals(3, e.stage)
    }

    // ---------- Language ----------

    @Test fun everyErrorHasPersianText() {
        val bad = listOf(
            "x = = 3", "print(1", "y = 5 5", "x = @", "print(\"abc)", "x = 5\nx = \"hi\"", "x = \"a\" - 1", "print(y)",
            "x = 1\nif x:\n    print(1)", "x = 1 < \"a\"", "x = -\"a\"", "import os", "break", "3 = x",
            "while True:\n    x = 1", "x = 1\nprint(x/0)", "x = 3\n  print(x)", "if True:\n    print(1)\n  print(2)",
            "else:\n    print(1)", "for i in range(x):\n    print(i)", "for i in range(1, 5, 0):\n    print(i)",
            "def f(n):\n    return f(n)\nprint(f(1))", "x = 3\nif x > 1\n    print(1)", "if True:\nprint(1)"
        )
        for (code in bad) {
            val en = ErrorExplainer.forOutcome(run(code), code, Lang.EN)
            val fa = ErrorExplainer.forOutcome(run(code), code, Lang.FA)
            assertTrue("no error for: $code", en.isNotEmpty())
            assertEquals(en.size, fa.size)
            val f = fa.first()
            assertTrue("Persian title missing for: $code -> ${f.title}", hasPersian(f.title))
            assertTrue("Persian explanation missing for: $code", hasPersian(f.explanation))
            assertTrue("Persian tip missing for: $code", hasPersian(f.tip))
            assertFalse("English title is Persian for: $code", hasPersian(en.first().title))
        }
    }

    @Test fun langPickAndCodes() {
        assertEquals("a", Lang.EN.pick("a", "b")); assertEquals("b", Lang.FA.pick("a", "b"))
        assertEquals(Lang.FA, Lang.fromCode(1)); assertEquals(Lang.EN, Lang.fromCode(0)); assertEquals(Lang.EN, Lang.fromCode(42))
    }

    @Test fun stageInfoIsBilingualForAllSixStages() {
        for (i in 0 until StageInfo.COUNT) for (lang in Lang.values()) {
            assertTrue(StageInfo.tab(i, lang).isNotBlank()); assertTrue(StageInfo.title(i, lang).isNotBlank())
            assertTrue(StageInfo.about(i, lang).isNotBlank()); assertTrue(StageInfo.tryThis(i, lang).isNotBlank())
        }
        for (i in 0 until StageInfo.COUNT) assertTrue(hasPersian(StageInfo.about(i, Lang.FA)))
    }

    @Test fun examplesAreBilingual() {
        for (ex in Examples.good + Examples.mistakes) {
            assertTrue(hasPersian(ex.name(Lang.FA)) || ex.nameFa.isNotBlank())
            assertTrue(hasPersian(ex.description(Lang.FA)))
        }
    }

    // ---------- Tree ----------

    @Test fun treeShowsStructureOfAnExpression() {
        val o = run("x = 1 + 2 * 3\nprint(x)\n")
        val root = AstTree.build(o.result!!.ast!!, o.lineMap, Lang.EN)
        val text = AstTree.renderText(root)
        assertTrue(text.startsWith("Program"))
        assertTrue(text.contains("Assign: x"))
        // * binds tighter than +, so * must be deeper than +
        val lines = AstTree.flatten(root)
        val plus = lines.indexOfFirst { it.label == "Operator  +" }
        val times = lines.indexOfFirst { it.label == "Operator  *" }
        assertTrue(plus >= 0 && times > plus)
        assertTrue(lines[times].prefix.length > lines[plus].prefix.length)
        assertTrue(text.contains("├── ") || text.contains("└── "))
        assertEquals(lines.size, AstTree.nodeCount(root))
    }

    @Test fun treeHandlesIfElseWhileAndPersian() {
        val code = "n = 0\nwhile n < 3:\n    if n == 1:\n        print(\"one\")\n    else:\n        print(n)\n    n = n + 1\n"
        val o = run(code)
        val en = AstTree.renderText(AstTree.build(o.result!!.ast!!, o.lineMap, Lang.EN))
        val fa = AstTree.renderText(AstTree.build(o.result!!.ast!!, o.lineMap, Lang.FA))
        for (k in listOf("While loop", "condition", "body", "If", "then", "else", "Text  \"one\"")) assertTrue(k, en.contains(k))
        assertTrue(fa.contains("حلقه While") && fa.contains("وگرنه") && fa.contains("برنامه"))
        assertTrue(en.contains("(line 2)") && en.contains("(line 3)"))
    }

    @Test fun treeUsesOriginalLineNumbersAfterFunctionExpansion() {
        val code = "def f(a):\n    return a + 1\n\nz = f(2)\nprint(z)\n"
        val o = run(code)
        val text = AstTree.renderText(AstTree.build(o.result!!.ast!!, o.lineMap, Lang.EN))
        assertTrue(text, text.contains("(line 4)") || text.contains("(line 2)"))
        assertFalse(text, text.contains("(line 9)"))
    }

    @Test fun treeNumbersAreFormattedNicely() {
        val o = run("x = 2.5\ny = 3\nprint(x + y)\n")
        val text = AstTree.renderText(AstTree.build(o.result!!.ast!!, o.lineMap, Lang.EN))
        assertTrue(text.contains("Number  2.5")); assertTrue(text.contains("Number  3")); assertFalse(text.contains("3.0"))
    }

    // ---------- Tables ----------

    @Test fun tokenTableHasOneRowPerTokenAndReadableCells() {
        val o = run("x = 3\nprint(\"hi\")\n")
        val r = o.result!!
        val t = StageTables.tokens(r.tokens, o.lineMap, Lang.EN)
        assertEquals(r.tokens.size, t.rows.size)
        assertEquals(t.headers.size, t.widths.size)
        assertTrue(t.rows.all { it.size == t.headers.size })
        assertTrue(t.rows.any { it[3] == "\"hi\"" && it[2] == "Text" })
        assertTrue(t.rows.any { it[3] == "↵" })
        assertTrue(t.rows.last()[3].contains("EOF"))
        assertTrue(t.rows.all { it[4].isNotBlank() })
        val fa = StageTables.tokens(r.tokens, o.lineMap, Lang.FA)
        assertTrue(hasPersian(fa.headers[2]) && hasPersian(fa.rows[0][2]))
    }

    @Test fun tokenTableShowsOriginalLineNumbers() {
        val o = run("def f(a):\n    return a\n\nz = f(1)\nprint(z)\n")
        val t = StageTables.tokens(o.result!!.tokens, o.lineMap, Lang.EN)
        val lines = t.rows.map { it[1] }.filter { it != "?" }.map { it.toInt() }
        assertTrue(lines.all { it in 1..6 })
    }

    @Test fun tokenTableIsCapped() {
        val big = "x = 0\n" + "x = x + 1\n".repeat(200)
        val o = run(big)
        assertEquals(400, StageTables.tokens(o.result!!.tokens, o.lineMap, Lang.EN).rows.size)
    }

    @Test fun otherTablesAreWellFormed() {
        val o = run("x = 3\ny = x * 2 + 1\nif y > 5:\n    print(\"big\")\nelse:\n    print(-y)\n")
        val r = o.result!!
        val sym = StageTables.symbols(r.symbols, o.lineMap, Lang.EN)
        assertEquals(listOf("x", "Integer", "global", "1"), sym.rows[0])
        val tac = StageTables.tac(r.tac, Lang.EN)
        assertEquals(r.tac.size, tac.rows.size)
        assertTrue(tac.rows.any { it[5] == "t1 = x * 2" })
        assertTrue(tac.rows.any { it[5].startsWith("if t") && it[5].contains("is false, go to") })
        val opt = StageTables.tac(r.optimizedTac, Lang.EN)
        assertTrue(opt.rows.size <= tac.rows.size)
        val bc = StageTables.bytecode(r.bytecode, Lang.EN)
        assertEquals(r.bytecode.size, bc.rows.size)
        assertTrue(bc.rows.all { it[3].isNotBlank() })
        assertEquals("HALT", bc.rows.last()[1])
        assertTrue(hasPersian(StageTables.bytecode(r.bytecode, Lang.FA).rows[0][3]))
    }

    // ---------- Report ----------

    @Test fun reportContainsErrorsAndOutputInBothLanguages() {
        val bad = "print(y)\n"
        val rep = Report.build(bad, run(bad), Lang.EN)
        assertTrue(rep.contains("Variable 'y' is used before it exists")); assertTrue(rep.contains("line 1"))
        assertTrue(hasPersian(Report.build(bad, run(bad), Lang.FA)))
        val good = Report.build("print(1)\n", run("print(1)\n"), Lang.EN)
        assertTrue(good.contains("Errors:\nNone") || good.contains("Errors:\r\nNone"))
        assertTrue(good.contains("Program output:"))
        assertTrue(Report.build("x", null, Lang.EN).contains("Nothing has been compiled"))
    }

    // ---------- Robustness ----------

    @Test fun neverThrowsOnWeirdInput() {
        val weird = listOf("", "\n\n\n", "   ", "\t\t", "((((", "))))", "\"", "'", "#", "if", "else:", "while", ":", "def", "def f(", "x = " + "9".repeat(60), "\u0000", "😀", "x = 1\u202E",
            "for i in range(99999999999):\n    print(i)\n", "if x:", "\n\tprint(1)")
        for (w in weird) {
            val o = run(w)
            ErrorExplainer.forOutcome(o, w, Lang.EN); ErrorExplainer.forOutcome(o, w, Lang.FA)
            Report.build(w, o, Lang.FA)
            o.result?.let {
                AstTree.flatten(AstTree.build(it.ast ?: com.mahdi.compilerstudio.compiler.Program(emptyList()), o.lineMap, Lang.EN))
                StageTables.tokens(it.tokens, o.lineMap, Lang.EN); StageTables.tac(it.tac, Lang.EN)
            }
        }
    }

    // ---------- "Did you mean ...?" ----------

    @Test fun suggestsTheClosestVariableName() {
        val code = "total = 1\nprint(totl)"
        assertEquals("total", ErrorExplainer.suggestName("totl", code))
        val en = ErrorExplainer.forOutcome(run(code), code, Lang.EN).first()
        assertTrue(en.tip, en.tip.contains("Did you mean 'total'?"))
        val fa = ErrorExplainer.forOutcome(run(code), code, Lang.FA).first()
        assertTrue(fa.tip, fa.tip.contains("'total'") && hasPersian(fa.tip))
    }

    @Test fun suggestsLoopVariableAndIgnoresCase() {
        assertEquals("count", ErrorExplainer.suggestName("Count", "count = 0\nprint(Count)"))
        assertEquals("i", ErrorExplainer.suggestName("j", "for i in range(3):\n    print(j)"))
    }

    @Test fun noSuggestionWhenNothingIsClose() {
        assertNull(ErrorExplainer.suggestName("banana", "total = 1\nprint(banana)"))
        val code = "print(y)"
        val tip = ErrorExplainer.forOutcome(run(code), code, Lang.EN).first().tip
        assertFalse(tip, tip.contains("Did you mean"))
    }
}
