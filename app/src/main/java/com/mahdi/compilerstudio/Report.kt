package com.mahdi.compilerstudio

/** Plain-text report that can be shared from the app. */
object Report {
    fun build(source: String, outcome: Pipeline.Outcome?, lang: Lang): String = buildString {
        appendLine(lang.pick("Compiler Studio — Compilation Report", "Compiler Studio — گزارش کامپایل"))
        appendLine("====================================")
        appendLine(lang.pick("Source code:", "کد منبع:"))
        appendLine(source)
        if (outcome == null) {
            appendLine(lang.pick("Nothing has been compiled yet.", "هنوز چیزی کامپایل نشده است."))
            return@buildString
        }
        if (outcome.loweredSource != null && outcome.loweredSource != source) {
            appendLine()
            appendLine(lang.pick("Lowered engine input:", "ورودی تبدیل‌شده برای موتور:"))
            appendLine(outcome.loweredSource)
        }
        val errors = ErrorExplainer.forOutcome(outcome, source, lang)
        val r = outcome.result
        appendLine()
        appendLine(lang.pick("Pipeline:", "مراحل:"))
        if (r == null) {
            appendLine(lang.pick("The code could not be read, so no stage ran.", "کد خوانده نشد، پس هیچ مرحله‌ای اجرا نشد."))
        } else {
            appendLine(lang.pick("Completed stages: ${r.completedPhase}/6", "مراحل کامل‌شده: ${r.completedPhase} از ۶"))
            appendLine(lang.pick("Tokens: ${r.tokens.size}", "توکن‌ها: ${r.tokens.size}"))
            appendLine(lang.pick("Variables: ${r.symbols.size}", "متغیرها: ${r.symbols.size}"))
            appendLine(lang.pick("TAC instructions: ${r.tac.size} (optimized: ${r.optimizedTac.size})", "دستورهای TAC: ${r.tac.size} (پس از بهینه‌سازی: ${r.optimizedTac.size})"))
            appendLine(lang.pick("Bytecode instructions: ${r.bytecode.size}", "دستورهای بایت‌کد: ${r.bytecode.size}"))
        }
        appendLine()
        appendLine(lang.pick("Errors:", "خطاها:"))
        if (errors.isEmpty()) appendLine(lang.pick("None", "ندارد"))
        errors.forEach { e ->
            val where = e.line?.let { lang.pick("line $it", "خط $it") } ?: ""
            appendLine("• ${e.title} $where".trimEnd())
            e.sourceLine?.let { appendLine("    $it") }
            appendLine("  ${e.explanation}")
            appendLine("  ${lang.pick("Tip", "راهنما")}: ${e.tip}")
            appendLine("  (${e.technical})")
        }
        if (r != null) {
            appendLine()
            appendLine(lang.pick("Program output:", "خروجی برنامه:"))
            appendLine(r.output)
        }
    }
}
