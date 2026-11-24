package com.mahdi.compilerstudio

import com.mahdi.compilerstudio.compiler.Diagnostic

/** An error rewritten for beginners: what happened, why, and how to fix it. */
data class FriendlyError(
    /** 0 = found while reading the code (before the six stages), 1..6 = the stage that reported it. */
    val stage: Int,
    /** Line in the user's original source, when known. */
    val line: Int?,
    val title: String,
    val explanation: String,
    val tip: String,
    /** The text of the offending line, when known. */
    val sourceLine: String?,
    /** The original technical message from the compiler. */
    val technical: String
)

object ErrorExplainer {

    fun stageOfPhase(phase: String): Int = when (phase) {
        "Lexer" -> 1
        "Parser" -> 2
        "Semantic" -> 3
        "IR Generation" -> 4
        "Optimization" -> 5
        else -> 6 // Code Generation, Runtime, VM
    }

    private data class Text3(val title: String, val explanation: String, val tip: String)

    /** Words from full Python that this educational language does not have. */
    private val unsupportedWords = setOf(
        "import", "from", "class", "return", "try", "except", "finally", "raise", "with", "lambda",
        "break", "continue", "pass", "global", "nonlocal", "input", "del", "assert", "yield", "async", "await"
    )

    fun fromDiagnostic(d: Diagnostic, lineMap: List<Int>, source: String, lang: Lang): FriendlyError {
        val generated = d.pos?.line
        val line = generated?.let { Pipeline.originalLine(lineMap, it) }
        val srcLine = line?.let { source.lines().getOrNull(it - 1) }
        var text = describeEngine(d.message, srcLine.orEmpty(), lang)
        Regex("^Undefined variable '(.*)'$").matchEntire(d.message)?.let { m ->
            suggestName(m.groupValues[1], source)?.let { s ->
                text = text.copy(
                    tip = text.tip + lang.pick("  Did you mean '$s'?", "  شاید منظورتان '$s' بود؟")
                )
            }
        }
        return FriendlyError(stageOfPhase(d.phase), line, text.title, text.explanation, text.tip, srcLine, d.message)
    }

    /**
     * "Did you mean ...?" – the variable name written in [source] (assigned with = or used as a
     * for-loop variable) that is closest to the misspelled [name], or null when none is close.
     */
    fun suggestName(name: String, source: String): String? {
        val known = LinkedHashSet<String>()
        val assign = Regex("^\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*=(?!=)")
        val loop = Regex("^\\s*for\\s+([A-Za-z_][A-Za-z0-9_]*)\\s+in\\b")
        source.lines().forEach { l ->
            assign.find(l)?.let { known += it.groupValues[1] }
            loop.find(l)?.let { known += it.groupValues[1] }
        }
        known.remove(name)
        val limit = if (name.length <= 3) 1 else 2
        return known
            .map { it to editDistance(it.lowercase(), name.lowercase()) }
            .filter { it.second <= limit }
            .minByOrNull { it.second }
            ?.first
    }

    private fun editDistance(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1)
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            prev = cur
        }
        return prev[b.length]
    }

    fun fromFrontend(issue: Pipeline.FrontendIssue, source: String, lang: Lang): FriendlyError {
        val line = issue.line.takeIf { it > 0 }
        val srcLine = line?.let { source.lines().getOrNull(it - 1) }
        val text = describeFrontend(issue.message, lang)
        return FriendlyError(0, line, text.title, text.explanation, text.tip, srcLine, issue.message)
    }

    /** All friendly errors of one run (frontend issue or engine diagnostics). */
    fun forOutcome(outcome: Pipeline.Outcome, source: String, lang: Lang): List<FriendlyError> {
        outcome.frontendIssue?.let { return listOf(fromFrontend(it, source, lang)) }
        val r = outcome.result ?: return emptyList()
        return r.diagnostics.map { fromDiagnostic(it, outcome.lineMap, source, lang) }
    }

    // ------------------------------------------------------------------ engine messages

    private fun describeEngine(raw: String, srcLine: String, lang: Lang): Text3 {
        val atEnd = raw.contains("found '\n'")
        val msg = raw.replace("'\n'", "'end of line'")

        Regex("^Invalid character '(.*)'$", RegexOption.DOT_MATCHES_ALL).matchEntire(raw)?.let {
            val c = it.groupValues[1]
            return Text3(
                lang.pick("Unknown character '$c'", "نویسه ناشناخته '$c'"),
                lang.pick(
                    "The compiler met a character that is not part of this language. Letters, digits, + - * / % = < > ! ( ) and quotation marks are allowed.",
                    "کامپایلر به نویسه‌ای رسید که جزو این زبان نیست. حروف، ارقام و + - * / % = < > ! ( ) و گیومه مجاز هستند."
                ),
                lang.pick(
                    "Delete or replace '$c'. Lists [ ], dictionaries and numbers like 2.5.1 are not supported.",
                    "'$c' را حذف یا جایگزین کنید. لیست [ ]، دیکشنری و اعدادی مثل 2.5.1 پشتیبانی نمی‌شوند."
                )
            )
        }
        if (raw.startsWith("Unterminated string literal")) return Text3(
            lang.pick("Text is missing its closing quote", "گیومه پایان متن جا افتاده"),
            lang.pick(
                "Text must start and end with a quotation mark (\") on the same line.",
                "متن باید با گیومه (\") شروع و در همان خط با گیومه تمام شود."
            ),
            lang.pick("Add a \" at the end of the text, e.g.  print(\"Hello\")", "یک \" در انتهای متن اضافه کنید، مثلاً  print(\"Hello\")")
        )

        Regex("^Expected expression, found '(.*)'$", RegexOption.DOT_MATCHES_ALL).matchEntire(raw)?.let {
            val found = it.groupValues[1]
            return if (atEnd) Text3(
                lang.pick("The line ends too early", "خط زودتر از موعد تمام شد"),
                lang.pick(
                    "A value (number, text, variable or calculation) was expected, but the line ended. This happens after =, +, -, *, / or an opening ( .",
                    "یک مقدار (عدد، متن، متغیر یا محاسبه) انتظار می‌رفت اما خط تمام شد. این مشکل بعد از = ، + ، - ، * ، / یا پرانتز باز پیش می‌آید."
                ),
                lang.pick("Finish the line, e.g.  x = 5 + 3", "خط را کامل کنید، مثلاً  x = 5 + 3")
            ) else Text3(
                lang.pick("A value is missing before '$found'", "قبل از '$found' یک مقدار جا افتاده"),
                lang.pick(
                    "After an operator or an opening bracket the compiler needs a value, but it found '$found' instead.",
                    "بعد از عملگر یا پرانتز باز، کامپایلر یک مقدار لازم دارد اما '$found' را دید."
                ),
                lang.pick("Remove the extra '$found' or put a number/variable before it.", "'$found' اضافه را پاک کنید یا قبل از آن یک عدد/متغیر بگذارید.")
            )
        }
        Regex("^Expected '\\)', found '(.*)'$", RegexOption.DOT_MATCHES_ALL).matchEntire(raw)?.let {
            val found = it.groupValues[1]
            return if (atEnd) Text3(
                lang.pick("Missing closing parenthesis  )", "پرانتز بسته  )  جا افتاده"),
                lang.pick("Every ( needs a matching ) before the line ends.", "هر ( باید قبل از پایان خط یک ) متناظر داشته باشد."),
                lang.pick("Add ) at the end of the line, e.g.  print(x)", "در انتهای خط ) اضافه کنید، مثلاً  print(x)")
            ) else Text3(
                lang.pick("Unexpected '$found' inside parentheses", "'$found' غیرمنتظره داخل پرانتز"),
                lang.pick(
                    "The compiler expected ) here. Two values next to each other need an operator between them, such as +.",
                    "کامپایلر اینجا انتظار ) داشت. دو مقدار کنار هم باید بین‌شان عملگر (مثل +) باشد."
                ),
                lang.pick("Put an operator between the values, or close the parenthesis before '$found'.", "بین مقادیر عملگر بگذارید یا قبل از '$found' پرانتز را ببندید.")
            )
        }
        Regex("^Expected '=', found '(.*)'$", RegexOption.DOT_MATCHES_ALL).matchEntire(raw)?.let {
            val word = srcLine.trim().takeWhile { c -> c.isLetterOrDigit() || c == '_' }
            return if (word in unsupportedWords) Text3(
                lang.pick("'$word' is not supported", "'$word' پشتیبانی نمی‌شود"),
                lang.pick(
                    "This educational language is a small subset of Python. '$word' is a real Python feature but it is not implemented here.",
                    "این زبان آموزشی زیرمجموعه کوچکی از پایتون است. '$word' در پایتون هست اما اینجا پیاده‌سازی نشده."
                ),
                lang.pick("Open the SYNTAX guide to see what you can use.", "راهنمای نحو (SYNTAX) را باز کنید تا ببینید چه چیزهایی مجاز است.")
            ) else Text3(
                lang.pick("Unknown command or incomplete assignment", "دستور ناشناخته یا انتساب ناقص"),
                lang.pick(
                    "A line that starts with a name must be an assignment like  name = value. The compiler read '${word.ifEmpty { "the first word" }}' as a variable name but did not find = after it.",
                    "خطی که با یک نام شروع می‌شود باید انتساب باشد، مثل  name = value. کامپایلر '${word.ifEmpty { "اولین کلمه" }}' را نام متغیر دید ولی بعدش = پیدا نکرد."
                ),
                lang.pick(
                    "Check the spelling of print / if / while / for (they are lowercase), or write  x = value.",
                    "املای print / if / while / for را بررسی کنید (حروف کوچک)، یا بنویسید  x = مقدار."
                )
            )
        }
        if (raw.startsWith("Expected ';'")) return Text3(
            lang.pick("Something extra after a complete statement", "چیز اضافه بعد از یک دستور کامل"),
            lang.pick(
                "The statement was already complete, but more text follows on the same line (for example a second value or a stray ) ).",
                "دستور کامل بود اما در همان خط چیز دیگری آمده (مثلاً یک مقدار دوم یا یک ) اضافه)."
            ),
            lang.pick("Remove the extra part, or move it to its own line. Check that parentheses are balanced.", "بخش اضافه را پاک کنید یا در خط جدا بنویسید. تعادل پرانتزها را بررسی کنید.")
        )
        Regex("^Unexpected token '(.*)'$", RegexOption.DOT_MATCHES_ALL).matchEntire(raw)?.let {
            val t = it.groupValues[1]
            return Text3(
                lang.pick("A line cannot start with '$t'", "یک خط نمی‌تواند با '$t' شروع شود"),
                lang.pick(
                    "Statements start with a variable name, print, if, while or for. For example  3 = x  is wrong, while  x = 3  is right.",
                    "دستورها با نام متغیر، print، if، while یا for شروع می‌شوند. مثلاً  3 = x  غلط است ولی  x = 3  درست است."
                ),
                lang.pick("Rewrite the line so it starts with a name or a command.", "خط را طوری بنویسید که با یک نام یا دستور شروع شود.")
            )
        }
        if (raw.startsWith("Expected '}'")) return Text3(
            lang.pick("A block was never closed", "یک بلاک بسته نشده"),
            lang.pick("A block opened with { but the closing } was not found.", "بلاکی با { باز شده اما } پایانی پیدا نشد."),
            lang.pick("Add the missing }, or use indentation-style blocks with a colon.", "} جا افتاده را اضافه کنید یا از بلاک‌های تورفتگی با دونقطه استفاده کنید.")
        )
        Regex("^Expected '(.*)', found '(.*)'$", RegexOption.DOT_MATCHES_ALL).matchEntire(raw)?.let {
            return Text3(
                lang.pick("Expected '${it.groupValues[1]}' here", "اینجا انتظار '${it.groupValues[1]}' می‌رفت"),
                lang.pick("The compiler expected '${it.groupValues[1]}' but found '${it.groupValues[2].replace("\n", "end of line")}'.", "کامپایلر '${it.groupValues[1]}' انتظار داشت اما '${it.groupValues[2].replace("\n", "پایان خط")}' را دید."),
                lang.pick("Compare this line with the examples.", "این خط را با مثال‌ها مقایسه کنید.")
            )
        }

        // ---- semantic
        Regex("^Undefined variable '(.*)'$").matchEntire(raw)?.let {
            val v = it.groupValues[1]
            return Text3(
                lang.pick("Variable '$v' is used before it exists", "متغیر '$v' قبل از ساخته شدن استفاده شده"),
                lang.pick(
                    "A variable must get a value (like  $v = 5 ) before you can read it.",
                    "یک متغیر باید اول مقدار بگیرد (مثل  $v = 5 ) و بعد خوانده شود."
                ),
                lang.pick(
                    "Check the spelling (capital letters matter) and make sure the assignment is above this line.",
                    "املا را بررسی کنید (حروف بزرگ و کوچک فرق دارند) و مطمئن شوید انتساب بالاتر از این خط است."
                )
            )
        }
        Regex("^Cannot assign (\\w+) to '(.*)' of type (\\w+)").find(raw)?.let {
            val newT = StageTables.typeName(it.groupValues[1], lang)
            val oldT = StageTables.typeName(it.groupValues[3], lang)
            val v = it.groupValues[2]
            return Text3(
                lang.pick("'$v' cannot change its type", "'$v' نمی‌تواند نوعش را عوض کند"),
                lang.pick(
                    "'$v' was first created as $oldT, and now you try to store $newT in it. In this language a variable keeps one type.",
                    "'$v' ابتدا از نوع $oldT ساخته شد و حالا می‌خواهید $newT در آن بگذارید. در این زبان هر متغیر یک نوع ثابت دارد."
                ),
                lang.pick("Use a different variable name for the new kind of value.", "برای نوع جدید مقدار از نام متغیر دیگری استفاده کنید.")
            )
        }
        if (raw.startsWith("Arithmetic operators require")) return Text3(
            lang.pick("Math on something that is not a number", "محاسبه روی چیزی که عدد نیست"),
            lang.pick(
                "- * / % only work with numbers. Text can only be joined with + (text + text).",
                "عملگرهای - * / % فقط روی عدد کار می‌کنند. متن را فقط می‌توان با + به متن دیگر چسباند."
            ),
            lang.pick("Make both sides numbers, or use + to join two texts.", "هر دو طرف را عدد کنید یا برای چسباندن دو متن از + استفاده کنید.")
        )
        Regex("^Incompatible comparison types (\\w+) and (\\w+)").find(raw)?.let {
            val a = StageTables.typeName(it.groupValues[1], lang)
            val b = StageTables.typeName(it.groupValues[2], lang)
            return Text3(
                lang.pick("Comparing $a with $b", "مقایسه $a با $b"),
                lang.pick("Two values can only be compared when they are the same kind.", "دو مقدار فقط وقتی قابل مقایسه‌اند که از یک نوع باشند."),
                lang.pick("Compare number with number, or text with text.", "عدد را با عدد و متن را با متن مقایسه کنید.")
            )
        }
        if (raw.startsWith("Cannot compare")) return Text3(
            lang.pick("These values cannot be compared", "این مقادیر قابل مقایسه نیستند"),
            lang.pick("Comparison needs two values of the same kind.", "مقایسه به دو مقدار هم‌نوع نیاز دارد."),
            lang.pick("Compare number with number, or text with text.", "عدد را با عدد و متن را با متن مقایسه کنید.")
        )
        if (raw.startsWith("Unary '-'")) return Text3(
            lang.pick("A minus sign needs a number", "علامت منفی به عدد نیاز دارد"),
            lang.pick("A leading - can only be placed in front of a number or a numeric variable.", "علامت - در ابتدا فقط جلوی عدد یا متغیر عددی می‌آید."),
            lang.pick("Remove the - or use a number.", "علامت - را بردارید یا از عدد استفاده کنید.")
        )
        Regex("^(.*) must be Boolean, got (\\w+)").find(raw)?.let {
            val got = StageTables.typeName(it.groupValues[2], lang)
            return Text3(
                lang.pick("A true/false value is needed here", "اینجا یک مقدار درست/نادرست لازم است"),
                lang.pick(
                    "${it.groupValues[1]} must be true or false, but it is $got. A condition is usually a comparison such as  x > 0.",
                    "این قسمت باید درست یا نادرست باشد اما $got است. شرط معمولاً یک مقایسه است، مثل  x > 0."
                ),
                lang.pick("Write a comparison, e.g.  if x != 0:", "یک مقایسه بنویسید، مثلاً  if x != 0:")
            )
        }
        if (raw.startsWith("Unsupported operator")) return Text3(
            lang.pick("Unsupported operator", "عملگر پشتیبانی‌نشده"),
            lang.pick("This operator is not available in the language.", "این عملگر در زبان وجود ندارد."),
            lang.pick("Use + - * / % , comparisons, and, or, not.", "از + - * / % ، مقایسه‌ها و and / or / not استفاده کنید.")
        )

        // ---- runtime
        if (raw.startsWith("Division by zero")) return Text3(
            lang.pick("Division by zero", "تقسیم بر صفر"),
            lang.pick("Dividing (or taking a remainder) by 0 has no answer, so the program stopped.", "تقسیم (یا باقیمانده) بر صفر جواب ندارد و برنامه متوقف شد."),
            lang.pick("Check that the divisor can never be 0, for example with an if.", "مطمئن شوید مقسوم‌علیه هرگز صفر نمی‌شود، مثلاً با یک if.")
        )
        if (raw.startsWith("Execution step limit")) return Text3(
            lang.pick("The program probably never stops", "برنامه احتمالاً هرگز تمام نمی‌شود"),
            lang.pick(
                "It ran 100,000 steps without finishing. This usually means a while loop whose condition never becomes false.",
                "برنامه ۱۰۰٬۰۰۰ گام اجرا شد و تمام نشد. معمولاً یعنی شرط یک حلقه while هرگز نادرست نمی‌شود."
            ),
            lang.pick(
                "Change the variable used in the while condition inside the loop, e.g.  counter = counter + 1",
                "داخل حلقه متغیری را که در شرط while هست تغییر دهید، مثلاً  counter = counter + 1"
            )
        )

        return Text3(
            msg,
            lang.pick("The compiler reported a problem that has no simpler explanation yet.", "کامپایلر مشکلی گزارش کرد که هنوز توضیح ساده‌تری برایش نداریم."),
            lang.pick("Look at the highlighted line and compare it with the examples.", "خط مشخص‌شده را با مثال‌ها مقایسه کنید.")
        )
    }

    // ------------------------------------------------------------------ frontend messages

    private fun describeFrontend(raw: String, lang: Lang): Text3 {
        if (raw.startsWith("Missing ':'")) return Text3(
            lang.pick("Missing colon ( : )", "دو نقطه ( : ) جا افتاده"),
            lang.pick(
                "A line that starts a block (if, elif, else, while, for) must end with a colon.",
                "خطی که یک بلاک را شروع می‌کند (if، elif، else، while، for) باید به : ختم شود."
            ),
            lang.pick("Add : at the end of this line, e.g.  if x > 1:", "در انتهای این خط : اضافه کنید، مثلاً  if x > 1:")
        )
        if (raw.startsWith("Unexpected indentation at top level")) return Text3(
            lang.pick("This line is indented but no block is open", "این خط تورفتگی دارد ولی بلاکی باز نیست"),
            lang.pick(
                "Only lines inside an if / while / for / def block may start with spaces. Often the line above is missing its colon, or this line has extra spaces.",
                "فقط خطوط داخل بلاک if / while / for / def می‌توانند با فاصله شروع شوند. معمولاً خط بالا دونقطه ندارد یا این خط فاصله اضافه دارد."
            ),
            lang.pick("Delete the leading spaces, or add the missing : on the line above.", "فاصله‌های ابتدای خط را پاک کنید یا : جا افتاده در خط بالا را اضافه کنید.")
        )
        if (raw.startsWith("Expected an indented")) return Text3(
            lang.pick("The block is empty", "بلاک خالی است"),
            lang.pick("After a line ending with : the next line must be indented to show what belongs to the block.", "بعد از خطی که به : ختم می‌شود، خط بعدی باید تورفتگی داشته باشد تا معلوم شود جزو بلاک است."),
            lang.pick("Indent the block lines with 4 spaces.", "خطوط بلاک را با ۴ فاصله تو بدهید.")
        )
        if (raw.startsWith("Inconsistent indentation") || raw.startsWith("Unexpected extra indentation")) return Text3(
            lang.pick("The indentation does not line up", "تورفتگی‌ها هم‌تراز نیستند"),
            lang.pick("All lines inside one block must start with exactly the same number of spaces.", "همه خطوط یک بلاک باید دقیقاً با تعداد فاصله یکسان شروع شوند."),
            lang.pick("Use 4 spaces for every line of the same block.", "برای همه خطوط یک بلاک ۴ فاصله بگذارید.")
        )
        if (raw.startsWith("Do not mix tabs")) return Text3(
            lang.pick("Tabs and spaces are mixed", "Tab و فاصله با هم مخلوط شده‌اند"),
            lang.pick("Indentation must use only spaces or only tabs in the whole program.", "تورفتگی در کل برنامه باید فقط با فاصله یا فقط با Tab باشد."),
            lang.pick("Replace the tabs with 4 spaces.", "Tabها را با ۴ فاصله عوض کنید.")
        )
        if (raw.startsWith("Branch has no matching if")) return Text3(
            lang.pick("else / elif without an if", "else / elif بدون if"),
            lang.pick("An else or elif must come right after an if (or elif) block at the same indentation.", "else یا elif باید درست بعد از بلاک if (یا elif) و هم‌تراز با آن بیاید."),
            lang.pick("Check that it lines up with its if.", "هم‌تراز بودن آن با if را بررسی کنید.")
        )
        if (raw.startsWith("Malformed or unsupported control-flow")) return Text3(
            lang.pick("This if / while / for line is not understood", "این خط if / while / for فهمیده نشد"),
            lang.pick("The header must look like  if condition:  while condition:  or  for i in range(...):", "سرخط باید شبیه  if شرط:  یا  while شرط:  یا  for i in range(...):  باشد."),
            lang.pick("Compare it with the examples.", "آن را با مثال‌ها مقایسه کنید.")
        )
        if (raw.startsWith("range()") || raw.startsWith("Only for-variable")) return Text3(
            lang.pick("This for / range() form is not supported", "این شکل for / range() پشتیبانی نمی‌شود"),
            lang.pick("Only  for name in range(a)  /  range(a, b)  /  range(a, b, step)  with whole-number literals (not variables) is supported. The step cannot be 0.", "فقط  for name in range(a)  یا  range(a, b)  یا  range(a, b, step)  با اعداد صحیح ثابت (نه متغیر) پشتیبانی می‌شود. گام نمی‌تواند 0 باشد."),
            lang.pick("Use plain numbers, e.g.  for i in range(1, 6):", "از عدد ثابت استفاده کنید، مثلاً  for i in range(1, 6):")
        )
        Regex("^Function '(.*)' expects (\\d+) arguments, got (\\d+)").find(raw)?.let {
            return Text3(
                lang.pick("Wrong number of arguments for '${it.groupValues[1]}'", "تعداد آرگومان‌های '${it.groupValues[1]}' درست نیست"),
                lang.pick("The function was defined with ${it.groupValues[2]} parameter(s) but called with ${it.groupValues[3]} value(s).", "تابع با ${it.groupValues[2]} پارامتر تعریف شده اما با ${it.groupValues[3]} مقدار صدا زده شد."),
                lang.pick("Give exactly ${it.groupValues[2]} value(s) when calling it.", "هنگام صدا زدن دقیقاً ${it.groupValues[2]} مقدار بدهید.")
            )
        }
        if (raw.contains("Recursive")) return Text3(
            lang.pick("Recursion is not supported", "بازگشتی (recursion) پشتیبانی نمی‌شود"),
            lang.pick("A function cannot call itself in this version, because functions are expanded before compiling.", "در این نسخه یک تابع نمی‌تواند خودش را صدا بزند چون توابع پیش از کامپایل باز می‌شوند."),
            lang.pick("Use a while loop instead.", "به‌جای آن از حلقه while استفاده کنید.")
        )
        if (raw.contains("Calling another function")) return Text3(
            lang.pick("A function cannot call another function yet", "تابع هنوز نمی‌تواند تابع دیگری را صدا بزند"),
            lang.pick("Calls to your own functions are only allowed in the main program, not inside another function.", "فراخوانی توابع خودتان فقط در برنامه اصلی مجاز است، نه داخل تابع دیگر."),
            lang.pick("Move the call out of the function body.", "فراخوانی را از بدنه تابع بیرون ببرید.")
        )
        if (raw.contains("does not return a value")) return Text3(
            lang.pick("This function gives back nothing", "این تابع مقداری برنمی‌گرداند"),
            lang.pick("You used the function's result, but it has no return line.", "از نتیجه تابع استفاده کردید اما return ندارد."),
            lang.pick("Add  return value  as the last line of the function.", "در آخرین خط تابع  return مقدار  بنویسید.")
        )
        if (raw.startsWith("return") || raw.startsWith("Only one final return")) return Text3(
            lang.pick("This return is not supported", "این return پشتیبانی نمی‌شود"),
            lang.pick("A function may have exactly one return, as its last line, not inside an if or loop, and it must give a value.", "تابع فقط می‌تواند یک return داشته باشد، در آخرین خط، نه داخل if یا حلقه، و حتماً با یک مقدار."),
            lang.pick("Compute the result in a variable, then  return result  at the end.", "نتیجه را در یک متغیر حساب کنید و در انتها  return result  بنویسید.")
        )
        if (raw.contains("already defined")) return Text3(
            lang.pick("Function name used twice", "نام تابع دوبار استفاده شده"),
            lang.pick("Each function needs a unique name.", "هر تابع نام یکتا لازم دارد."),
            lang.pick("Rename one of them.", "یکی را تغییر نام دهید.")
        )
        if (raw.startsWith("Function") || raw.startsWith("Malformed function") || raw.startsWith("Duplicate function") ||
            raw.startsWith("Unbalanced") || raw.startsWith("Empty function")
        ) return Text3(
            lang.pick("Problem with a function", "مشکلی در تابع"),
            lang.pick("Functions look like  def name(a, b):  followed by an indented body, and are called like  name(1, 2).", "توابع شبیه  def name(a, b):  هستند و بعدش بدنه تورفته می‌آید؛ صدا زدن: name(1, 2)."),
            lang.pick("Compare with the Functions example.", "با مثال توابع (Functions) مقایسه کنید.")
        )
        return Text3(
            raw,
            lang.pick("The code could not be read, and this problem has no simpler explanation yet.", "کد خوانده نشد و هنوز توضیح ساده‌تری برای این مشکل نداریم."),
            lang.pick("Check the highlighted line.", "خط مشخص‌شده را بررسی کنید.")
        )
    }
}
