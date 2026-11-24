package com.mahdi.compilerstudio

import com.mahdi.compilerstudio.compiler.ByteInstruction
import com.mahdi.compilerstudio.compiler.Kind
import com.mahdi.compilerstudio.compiler.SymbolInfo
import com.mahdi.compilerstudio.compiler.Tac
import com.mahdi.compilerstudio.compiler.Token

/** A plain table: header names, rows, and a suggested width (dp) per column. */
data class Table(
    val headers: List<String>,
    val rows: List<List<String>>,
    val widths: List<Int>,
    /** Index of a column that holds sentences (shown in the normal font, not monospace); -1 = none. */
    val proseColumn: Int = -1
)

object StageTables {

    // ---------- Stage 1: tokens ----------

    fun kindName(kind: Kind, lang: Lang): String = when (kind) {
        Kind.KEYWORD -> lang.pick("Keyword", "کلمه کلیدی")
        Kind.IDENT -> lang.pick("Name", "نام")
        Kind.NUMBER -> lang.pick("Number", "عدد")
        Kind.STRING -> lang.pick("Text", "متن")
        Kind.OP -> lang.pick("Operator", "عملگر")
        Kind.SYMBOL -> lang.pick("Symbol", "نماد")
        Kind.NEWLINE -> lang.pick("New line", "خط جدید")
        Kind.EOF -> lang.pick("End", "پایان")
    }

    fun displayLexeme(token: Token): String = when (token.kind) {
        Kind.NEWLINE -> "↵"
        Kind.EOF -> "∎ EOF"
        Kind.STRING -> "\"" + token.lexeme + "\""
        else -> token.lexeme
    }

    fun tokenMeaning(token: Token, lang: Lang): String = when (token.kind) {
        Kind.KEYWORD -> when (token.lexeme) {
            "if" -> lang.pick("Starts a condition", "شروع یک شرط")
            "else" -> lang.pick("Runs when the condition is false", "اجرا وقتی شرط نادرست است")
            "while" -> lang.pick("Repeats while the condition is true", "تکرار تا وقتی شرط درست است")
            "print" -> lang.pick("Shows a value on the screen", "نمایش یک مقدار")
            "and" -> lang.pick("Both sides must be true", "هر دو طرف باید درست باشند")
            "or" -> lang.pick("At least one side must be true", "حداقل یک طرف باید درست باشد")
            "not" -> lang.pick("Flips true and false", "درست و نادرست را برعکس می‌کند")
            "true", "false" -> lang.pick("A yes/no value", "یک مقدار منطقی (بله/خیر)")
            else -> lang.pick("Reserved word", "کلمه رزروشده")
        }
        Kind.IDENT -> lang.pick("A variable name", "نام یک متغیر")
        Kind.NUMBER -> lang.pick("A number value", "یک مقدار عددی")
        Kind.STRING -> lang.pick("A piece of text", "یک تکه متن")
        Kind.OP -> when (token.lexeme) {
            "+" -> lang.pick("Add (or join text)", "جمع (یا چسباندن متن)")
            "-" -> lang.pick("Subtract / negative", "تفریق / منفی")
            "*" -> lang.pick("Multiply", "ضرب")
            "/" -> lang.pick("Divide", "تقسیم")
            "%" -> lang.pick("Remainder after dividing", "باقیمانده تقسیم")
            "=" -> lang.pick("Store a value in a variable", "ذخیره مقدار در متغیر")
            "==" -> lang.pick("Is equal to?", "مساوی است؟")
            "!=" -> lang.pick("Is not equal to?", "نامساوی است؟")
            "<" -> lang.pick("Is smaller than?", "کوچک‌تر است؟")
            ">" -> lang.pick("Is bigger than?", "بزرگ‌تر است؟")
            "<=" -> lang.pick("Smaller or equal?", "کوچک‌تر یا مساوی؟")
            ">=" -> lang.pick("Bigger or equal?", "بزرگ‌تر یا مساوی؟")
            else -> lang.pick("Operator", "عملگر")
        }
        Kind.SYMBOL -> when (token.lexeme) {
            "(" -> lang.pick("Opens a group", "شروع پرانتز")
            ")" -> lang.pick("Closes a group", "پایان پرانتز")
            "{" -> lang.pick("Block starts (added from your indentation)", "شروع بلاک (از روی تورفتگی شما)")
            "}" -> lang.pick("Block ends (added from your indentation)", "پایان بلاک (از روی تورفتگی شما)")
            ";" -> lang.pick("End of statement", "پایان دستور")
            "," -> lang.pick("Separator", "جداکننده")
            else -> lang.pick("Symbol", "نماد")
        }
        Kind.NEWLINE -> lang.pick("End of a line", "پایان یک خط")
        Kind.EOF -> lang.pick("End of the program", "پایان برنامه")
    }

    fun tokens(tokens: List<Token>, lineMap: List<Int>, lang: Lang, maxRows: Int = 400): Table {
        val shown = tokens.take(maxRows)
        return Table(
            headers = listOf("#", lang.pick("Line", "خط"), lang.pick("Kind", "نوع"), lang.pick("Text", "متن"), lang.pick("Meaning", "معنی")),
            rows = shown.mapIndexed { i, t ->
                val line = t.pos?.line?.let { Pipeline.originalLine(lineMap, it) }?.toString() ?: "?"
                listOf((i + 1).toString(), line, kindName(t.kind, lang), displayLexeme(t), tokenMeaning(t, lang))
            },
            widths = listOf(38, 44, 92, 96, 250),
            proseColumn = 4
        )
    }

    // ---------- Stage 3: symbols ----------

    fun symbols(symbols: List<SymbolInfo>, lineMap: List<Int>, lang: Lang): Table = Table(
        headers = listOf(lang.pick("Name", "نام"), lang.pick("Type", "نوع"), lang.pick("Scope", "حوزه"), lang.pick("Line", "خط")),
        rows = symbols.map { s ->
            listOf(
                s.name,
                typeName(s.type.name, lang),
                if (s.scope == 0) lang.pick("global", "سراسری") else s.scope.toString(),
                Pipeline.originalLine(lineMap, s.line).toString()
            )
        },
        widths = listOf(110, 110, 90, 60)
    )

    fun typeName(raw: String, lang: Lang): String = when (raw) {
        "INT" -> lang.pick("Integer", "عدد صحیح")
        "FLOAT" -> lang.pick("Decimal", "اعشاری")
        "STRING" -> lang.pick("Text", "متن")
        "BOOL" -> lang.pick("Boolean", "منطقی")
        else -> raw
    }

    // ---------- Stage 4 / 5: three-address code ----------

    private val binaryOps = setOf("+", "-", "*", "/", "%", "==", "!=", "<", ">", "<=", ">=", "and", "or")

    /** Plain-language reading of one TAC instruction, e.g. "t1 = x * 2". */
    fun tacMeaning(t: Tac, lang: Lang): String = when {
        t.op == "MOV" -> "${t.out} = ${t.a}"
        t.op == "PRINT" -> lang.pick("show ${t.a}", "نمایش ${t.a}")
        t.op == "NEG" -> "${t.out} = -${t.a}"
        t.op == "NOT" -> "${t.out} = not ${t.a}"
        t.op == "LABEL" -> lang.pick("label ${t.out}", "برچسب ${t.out}")
        t.op == "JMP" -> lang.pick("go to ${t.out}", "پرش به ${t.out}")
        t.op == "JZ" -> lang.pick("if ${t.a} is false, go to ${t.out}", "اگر ${t.a} نادرست بود، پرش به ${t.out}")
        t.op in binaryOps -> "${t.out} = ${t.a} ${t.op} ${t.b}"
        else -> ""
    }

    fun tac(list: List<Tac>, lang: Lang): Table = Table(
        headers = listOf("#", lang.pick("Op", "عملیات"), "A", "B", lang.pick("Result", "نتیجه"), lang.pick("Reads as", "به زبان ساده")),
        rows = list.mapIndexed { i, t -> listOf((i + 1).toString(), t.op, t.a, t.b, t.out, tacMeaning(t, lang)) },
        widths = listOf(38, 70, 70, 70, 70, 230),
        proseColumn = 5
    )

    // ---------- Stage 6: bytecode ----------

    fun bytecodeMeaning(b: ByteInstruction, lang: Lang): String = when (b.op) {
        "LOAD" -> lang.pick("Push ${b.operand} on the stack", "قرار دادن ${b.operand} روی پشته")
        "STORE" -> lang.pick("Pop the top into ${b.operand}", "برداشتن بالاترین مقدار و ذخیره در ${b.operand}")
        "ADD" -> lang.pick("Pop two, push their sum", "دو مقدار را برداشته و حاصل جمع را می‌گذارد")
        "SUB" -> lang.pick("Pop two, push the difference", "دو مقدار را برداشته و تفاضل را می‌گذارد")
        "MUL" -> lang.pick("Pop two, push the product", "دو مقدار را برداشته و حاصل ضرب را می‌گذارد")
        "DIV" -> lang.pick("Pop two, push the quotient", "دو مقدار را برداشته و خارج قسمت را می‌گذارد")
        "MOD" -> lang.pick("Pop two, push the remainder", "دو مقدار را برداشته و باقیمانده را می‌گذارد")
        "EQ", "NE", "LT", "GT", "LE", "GE" -> lang.pick("Pop two, push true/false (comparison)", "دو مقدار را مقایسه و درست/نادرست می‌گذارد")
        "AND", "OR" -> lang.pick("Pop two, push true/false (logic)", "دو مقدار را با منطق ترکیب می‌کند")
        "NEG" -> lang.pick("Negate the top value", "علامت بالاترین مقدار را عوض می‌کند")
        "NOT" -> lang.pick("Flip true/false on top", "درست/نادرست بالای پشته را برعکس می‌کند")
        "PRINT" -> lang.pick("Pop and show the value", "برداشتن و نمایش مقدار")
        "JMP" -> lang.pick("Jump to ${b.operand}", "پرش به ${b.operand}")
        "JZ" -> lang.pick("Pop; if false, jump to ${b.operand}", "برداشتن؛ اگر نادرست بود، پرش به ${b.operand}")
        "LABEL" -> lang.pick("Jump target ${b.operand}", "مقصد پرش ${b.operand}")
        "HALT" -> lang.pick("Stop the machine", "توقف ماشین")
        else -> ""
    }

    fun bytecode(list: List<ByteInstruction>, lang: Lang): Table = Table(
        headers = listOf("#", lang.pick("Instruction", "دستور"), lang.pick("Operand", "عملوند"), lang.pick("What it does", "کار آن")),
        rows = list.mapIndexed { i, b -> listOf((i + 1).toString(), b.op, b.operand, bytecodeMeaning(b, lang)) },
        widths = listOf(38, 90, 90, 290),
        proseColumn = 3
    )
}
