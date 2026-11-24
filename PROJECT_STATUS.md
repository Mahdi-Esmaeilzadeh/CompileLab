# Compiler Studio — project status

## 1.9.1 verification increment

- [x] Corrected the expected output in the final-return-after-nested-control-flow test: the returned value is printed by the caller, so output is `positive\n2\n`.
- [x] Compiled both frontends and all test sources against the bundled compiler engine.
- [x] Executed all 46 test methods in a local JVM harness: 46 passed, 0 failed.
- [ ] Android APK build remains pending: this execution environment has no Android SDK or Gradle installation, and external dependency downloads are unavailable.

## 1.9.0 engineering increment

- [x] Reject nested/conditional `return` statements instead of silently changing their control-flow semantics during compile-time expansion.
- [x] Reject `print(user_function())` when the function has no return value.
- [x] Add regression coverage for both invalid cases and a valid unconditional return following nested control flow.
- [x] Compiled both frontends against the bundled engine and passed a direct JVM regression harness covering function returns, conditional-return rejection, void-function diagnostics, nested control flow, if/else, while, ascending/descending range loops, booleans, floats, and strings.
- [x] Verified the release archive contents and ZIP integrity.
- [ ] Run the complete JUnit suite in Android Studio/Gradle; this environment did not have the Gradle executable or cached JUnit dependency.


## Completed

- [x] Advanced the app to 1.8.0 (versionCode 9).
- [x] Added composed source-line maps across function expansion and indentation lowering.
- [x] Mapped lexer token positions, symbol declaration lines, and compiler diagnostics back to the original user source.
- [x] Mapped indentation frontend errors back through function expansion to original source line numbers.
- [x] Added four source-map regression tests; full direct JVM regression suite: 43/43 passed.

## Completed

- [x] Advanced the app to 1.7.0 (versionCode 8).
- [x] Added top-level function definitions with simple positional parameters.
- [x] Added function calls as statements, assignment right-hand sides, and direct `print(function(...))` arguments.
- [x] Added one final unconditional `return expression`; calls can assign returned values.
- [x] Isolated parameter and local-variable slots per expansion while allowing reads of globals.
- [x] Added diagnostics for arity mismatch, duplicate declarations/parameters, recursion, nested user-function calls, and unsupported return forms.
- [x] Added a function example and updated the in-app language guide.
- [x] Ran the complete direct JVM regression suite: 39/39 tests passed.


- [x] Advanced the app to 1.6.0 (versionCode 7).
- [x] Added an indentation frontend for Python-like `if`, `elif`, `else`, `while`, and literal `range()`-based `for` suites; existing brace-based source remains supported.
- [x] Added indentation diagnostics for unexpected/inconsistent indentation, missing block bodies, unmatched branches, and mixed tabs/spaces.
- [x] Normalized Python boolean literals `True` / `False` for the educational engine.
- [x] Added a preview/copy dialog for the lowered source passed to the six-stage engine.
- [x] Added revision guards so asynchronous results from an older editor snapshot cannot overwrite newer edits.
- [x] Added thirteen frontend integration tests, including ascending/descending/nested range loops, and ran the full JVM test harness (25/25 engine + frontend test methods passed).

- [x] Advanced the app to 1.4.0 (versionCode 5).
- [x] Added a built-in language syntax guide that clearly separates supported syntax from unsupported Python features.
- [x] Added nested-control-flow sample and verified modulo, precedence, comments, and division against the recovered engine.
- [x] Added source/output copy actions and a shareable compilation report containing source, pipeline counts, diagnostics, and VM output.
- [x] Preserved source text and selected pipeline stage across Android activity recreation.
- [x] Invalidated old compiler output whenever source text changes, preventing stale results from being mistaken for the current program.
- [x] Expanded the JUnit suite from 9 to 12 tests.

- [x] Recovered the 68,750-byte compiler engine JAR and inspected its class inventory.
- [x] Confirmed the engine's public API and all six compiler stages.
- [x] Ran JVM smoke checks for arithmetic, assignments, `print`, `if` / `else`, `while`, booleans, floats, strings, nested control flow, optimization, and diagnostics.
- [x] Integrated the recovered engine into the Android application; the UI invokes the engine rather than generating placeholder stage data.
- [x] Improved the editor workflow with three runnable examples, reset, asynchronous compilation, loading state, pipeline navigation, item counts, and stage-specific diagnostics.
- [x] Expanded the JUnit suite to nine focused compiler/VM tests.
- [x] Updated application version to 1.2.0 (versionCode 3).
- [ ] Android Gradle build and APK installation have not been verified in this environment.

## Known limitations

- The compiler is an educational language subset, not full Python. The adapter lowers supported indentation blocks into the engine brace syntax. Original-source line mapping is implemented for tokens, symbol lines, compiler diagnostics, and frontend indentation errors; precise column mapping through generated text is not yet implemented.
- Functions are compile-time expanded, not executed through runtime call frames. Recursion, nested user-function calls, conditional/multiple/early returns, default/keyword arguments, nested definitions, and arbitrary nested call expressions are not implemented.
- Collection iteration, lists, dictionaries, imports, classes, exceptions, `input()`, `break`, `continue`, expression-based `range()` arguments, and complete Python semantics are not implemented.
- The v4 archive metadata was found, but its original bytes could not be recovered in the workspace. This project is not claimed to be a byte-for-byte reconstruction of v4.
- This package contains `gradle-wrapper.properties` but not `gradlew`, `gradlew.bat`, or `gradle-wrapper.jar`, because these were absent from the recovered source archive. Open through Android Studio and select a local/downloaded Gradle 8.11.1 distribution, or use an installed Gradle 8.11.1 command-line executable.

## Next engineering milestones

1. Replace compile-time function expansion with native function definitions, call frames, return instructions, and lexical scopes across the parser, semantic analyzer, TAC, code generator, and VM.
2. Add editor line numbers and clickable source-position highlighting; extend mapping from line-level to exact source columns.
3. Add lists, iterators, and collection-based `for` loops with explicit memory/value semantics.
4. Expand diagnostics and regression coverage for malformed expressions, type errors, and runtime failures.
