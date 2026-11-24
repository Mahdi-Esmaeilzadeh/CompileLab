# Changelog

## 1.10.0

Beginner-friendly release.

- New: English / Persian (فارسی) interface switch (top-right button, remembered). Persian mode uses right-to-left layout; code, tables and trees always stay left-to-right.
- New: Lexer **token table** (#, line, kind, text, plain-language meaning).
- New: **syntax tree diagram** (├── └── branches, colour-coded) for the Parser stage.
- New: tables for symbols, TAC, optimized TAC (with before/after step count) and bytecode, each instruction explained in plain words.
- New: **friendly error cards**: what happened, which line (with the line shown), why, and how to fix it, in both languages, for every Lexer / Parser / Semantic / Runtime / frontend error. Includes "Did you mean 'total'?" for misspelled variables.
- New: "What is this stage?" explanation for each of the six stages, welcome card, "Try a mistake" examples, more examples (Factorial, FizzBuzz, Text), code size button, paste/clear buttons, whole screen scrolls (small phones, keyboard).
- Fixed: missing `:` after `if/elif/while/for/else` now says so, instead of a misleading "unexpected indentation" error.
- Fixed: parse errors no longer show a raw newline (`found '\n'`); they say "end of line".
- Code structure: pipeline, error explainer, tables, tree and examples are pure Kotlin (unit-tested); UI split into `MainActivity.kt` and `Ui.kt`.
- Tests: 73 (was 48), all passing under JUnit 4.13.2 on the JVM. Compose UI code could not be compiled in the build environment (no Android SDK); verify with `assembleDebug`.
- versionName 1.10.0, versionCode 13.

## 1.9.2

- Fixed: tabs and spaces mixed across different lines are now rejected (previously only mixing within a single line was caught). Added 2 regression tests (48 total).
- Fixed: header label showed "1.8"; now shows the current version.
- Added: Gradle Wrapper files (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) generated with the official `gradle wrapper` task (distribution 8.11.1).
- Not verified in this release: Gradle Sync, `assembleDebug`, APK, device run (no Android SDK was available in the build environment). See BUILD_REPORT_FA.md.

## 1.9.1-recovered
- Corrected a faulty test expectation for a function that executes nested control flow and then returns a value; the caller's print correctly emits both `positive` and `2`. No runtime behavior was changed for this correction.
- Compiled both frontends and all test sources against the bundled engine.
- Executed all 46 test methods in a local JVM harness: 46 passed, 0 failed.
- Updated application versionCode to 11.
- Android APK build is still pending in an Android SDK-equipped environment.

## 1.9.0-recovered
- Fixed a function-expansion correctness bug: a `return` nested inside `if`/`while` is now rejected instead of being incorrectly hoisted out of its branch.
- Enforced the documented unconditional-return rule by checking the return's indentation against the function body's indentation.
- Added a clear frontend diagnostic when `print(function())` is used with a function that has no return value.
- Added regression tests for conditional returns, void-function printing, and valid final returns after nested control flow.
- Updated application versionCode to 10.


## 1.8.0-recovered
- Added composed source-line maps across function expansion and indentation lowering.
- Compiler diagnostics, token positions, and symbol declaration lines now map back to the user's original editor source instead of generated engine input.
- Frontend indentation errors after function expansion are translated back to original source line numbers.
- Synthetic braces, loop increments, parameter setup, and expanded function-body instructions retain originating source-line metadata.
- Added four source-map regression tests; full direct JVM suite: 43/43 tests passed.
- Updated application versionCode to 9.


## 1.7.0-recovered
- Added top-level `def` declarations with simple positional parameters.
- Added statement-position function calls, assignment calls, and direct `print(function(...))` calls.
- Added one final unconditional `return expression` per function.
- Isolated parameters and local assignments with unique expansion slots; global reads remain visible.
- Added explicit diagnostics for wrong argument counts, duplicate definitions/parameters, recursion, nested user-function calls, and unsupported return forms.
- Added a runnable function example and updated the in-app syntax guide.
- Direct JVM regression suite: 39/39 engine + indentation + function tests passed.
- Updated application versionCode to 8.


## 1.6.0-recovered
- Added indentation-based `for variable in range(...)` loops, lowered to the existing VM's initialization / `while` / increment operations.
- Supports one-, two-, and three-argument ranges with 32-bit integer literals, including descending ranges with negative steps.
- Added nested range-loop examples and regression tests; zero steps and non-literal arguments are rejected explicitly.
- Full direct JVM test harness: 25/25 engine + frontend test methods passed.
- Updated application versionCode to 7.

## 1.5.0-recovered
- Added a Python-like indentation frontend for `if`, `elif`, `else`, and `while` blocks.
- Kept the previous brace-based syntax backward-compatible.
- Added indentation validation, missing-suite checks, orphan-branch diagnostics, and mixed-tab/space rejection.
- Added `True` / `False` literal normalization.
- Added a lowered-engine-input preview and copy action so the frontend transformation is inspectable.
- Prevented stale asynchronous compilation results from replacing results for newer editor contents.
- Added eight frontend integration tests; all 20 engine + frontend test methods passed in the direct JVM test harness.
- Updated application versionCode to 6.


## 1.4.0-recovered
- Preserve source text and selected pipeline stage across Android activity recreation.
- Clear previous compilation results whenever the editor content changes.
- Show the current source line count.
- Updated application versionCode to 5.

## 1.3.0-recovered
- Added an in-app syntax guide and an explicit list of unsupported Python features.
- Added a nested control-flow example.
- Added copy-source and copy-output actions.
- Added shareable compilation reports with source, pipeline metrics, diagnostics, and program output.
- Added tests for modulo, arithmetic precedence, comments, and division.
- Updated application versionCode to 4.

## 1.2.0-recovered
- Connected the six-stage UI to the recovered compiler engine.
- Added background compilation, stage inspection, and nine engine tests.
