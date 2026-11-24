package com.mahdi.compilerstudio

import com.mahdi.compilerstudio.compiler.Compiler as EngineCompiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionFrontendTest {
    private fun run(source: String): String {
        val functions = FunctionFrontend.translate(source)
        val indentation = IndentationFrontend.translate(functions.source)
        val result = EngineCompiler.compile(indentation.source, 100_000)
        assertEquals("Compiler failed: ${result.diagnostics}", null, result.failedPhase)
        return result.output
    }

    @Test fun expandsFunctionWithReturnAndArguments() {
        assertEquals("13\n", run("""def add(a, b):
    total = a + b
    return total

answer = add(8, 5)
print(answer)
"""))
    }

    @Test fun keepsFunctionLocalVariablesIsolatedBetweenCalls() {
        assertEquals("5\n9\n", run("""def calculate(value):
    temp = value + 1
    return temp

first = calculate(4)
second = calculate(8)
print(first)
print(second)
"""))
    }

    @Test fun expandsFunctionInsideConditionalBlock() {
        assertEquals("7\n", run("""def double(n):
    return n * 2

if True:
    result = double(3) + 1
    print(result)
"""))
    }

    @Test fun expandsFunctionWithoutReturnAsStatement() {
        assertEquals("Hello\n", run("""def greet(name):
    print(name)

greet("Hello")
"""))
    }


    @Test fun parameterReassignmentStaysLocalToFunctionExpansion() {
        assertEquals("12\n4\n", run("""def adjust(value):
    value = value + 8
    return value

original = 4
changed = adjust(original)
print(changed)
print(original)
"""))
    }

    @Test fun functionCanReadGlobalVariables() {
        assertEquals("15\n", run("""base = 10
def add_base(value):
    return base + value

print(add_base(5))
"""))
    }


    @Test fun functionCallInsideWhileBlockExpandsAtCorrectIndentation() {
        assertEquals("0\n2\n4\n", run("""def twice(value):
    return value * 2

index = 0
while index < 3:
    print(twice(index))
    index = index + 1
"""))
    }


    @Test fun rejectsConditionalReturnInsteadOfHoistingItOutOfTheBranch() {
        val source = """def choose(value):
    if value > 0:
        return value

answer = choose(1)
print(answer)
"""
        try {
            FunctionFrontend.translate(source)
            throw AssertionError("Expected conditional-return error")
        } catch (error: FunctionFrontend.FrontendException) {
            assertTrue(error.message!!.contains("unconditional"))
            assertTrue(error.message!!.contains("Line 3"))
        }
    }

    @Test fun rejectsPrintingAFunctionThatHasNoReturnValue() {
        val source = """def announce():
    print("ready")

print(announce())
"""
        try {
            FunctionFrontend.translate(source)
            throw AssertionError("Expected missing-return-value error")
        } catch (error: FunctionFrontend.FrontendException) {
            assertTrue(error.message!!.contains("does not return a value"))
        }
    }

    @Test fun allowsFinalUnconditionalReturnAfterNestedControlFlow() {
        assertEquals("positive\n2\n", run("""def classify(value):
    if value > 0:
        print("positive")
    return value

result = classify(2)
print(result)
"""))
    }

    @Test fun rejectsBareReturn() {
        try {
            FunctionFrontend.translate("def f():\n    return\n\nf()\n")
            throw AssertionError("Expected return-expression error")
        } catch (error: FunctionFrontend.FrontendException) {
            assertTrue(error.message!!.contains("must include a value"))
        }
    }

    @Test fun rejectsDuplicateFunctionParameters() {
        try {
            FunctionFrontend.translate("def f(value, value):\n    return value\n\nf(1, 2)\n")
            throw AssertionError("Expected duplicate parameter error")
        } catch (error: FunctionFrontend.FrontendException) {
            assertTrue(error.message!!.contains("Duplicate function parameter"))
        }
    }

    @Test fun rejectsCallingAnotherUserFunctionFromFunctionBody() {
        val source = "def first(x):\n    return x\ndef second(y):\n    return first(y)\n\nsecond(2)\n"
        try {
            FunctionFrontend.translate(source)
            throw AssertionError("Expected nested function-call error")
        } catch (error: FunctionFrontend.FrontendException) {
            assertTrue(error.message!!.contains("Calling another function"))
        }
    }


    @Test fun supportsNestedParenthesesInsideArguments() {
        assertEquals("49\n", run("""def square(value):
    return value * value

answer = square((3 + 4))
print(answer)
"""))
    }

    @Test fun rejectsWrongArgumentCount() {
        try {
            FunctionFrontend.translate("def add(a, b):\n    return a + b\n\nadd(1)\n")
            throw AssertionError("Expected argument-count error")
        } catch (error: FunctionFrontend.FrontendException) {
            assertTrue(error.message!!.contains("expects 2 arguments"))
        }
    }

    @Test fun rejectsRecursionAndNestedFunctionCalls() {
        val recursive = "def f(n):\n    return f(n)\n\nf(1)\n"
        try {
            FunctionFrontend.translate(recursive)
            throw AssertionError("Expected recursion error")
        } catch (error: FunctionFrontend.FrontendException) {
            assertTrue(error.message!!.contains("Recursive"))
        }
    }

    @Test fun rejectsNonFinalReturn() {
        try {
            FunctionFrontend.translate("def f(x):\n    return x\n    print(x)\n\nf(2)\n")
            throw AssertionError("Expected return-position error")
        } catch (error: FunctionFrontend.FrontendException) {
            assertTrue(error.message!!.contains("final return"))
        }
    }
}
