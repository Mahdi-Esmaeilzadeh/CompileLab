package com.mahdi.compilerstudio

import com.mahdi.compilerstudio.compiler.Expr
import com.mahdi.compilerstudio.compiler.Program
import com.mahdi.compilerstudio.compiler.Stmt

enum class NodeKind { ROOT, STATEMENT, GROUP, OPERATOR, LITERAL, VARIABLE }

data class TreeNode(val label: String, val kind: NodeKind, val children: List<TreeNode> = emptyList())

/** One printable row of the tree diagram: [prefix] holds the branch lines (├── │ └──). */
data class TreeLine(val prefix: String, val label: String, val kind: NodeKind)

/** Turns the engine's AST into a labelled tree and into a drawable list of rows. */
object AstTree {
    fun build(program: Program, lineMap: List<Int>, lang: Lang): TreeNode =
        TreeNode(lang.pick("Program", "برنامه"), NodeKind.ROOT, program.statements.map { stmt(it, lineMap, lang) })

    private fun lineOf(pos: com.mahdi.compilerstudio.compiler.SourcePos?, lineMap: List<Int>): Int? =
        pos?.line?.let { Pipeline.originalLine(lineMap, it) }

    private fun lineSuffix(pos: com.mahdi.compilerstudio.compiler.SourcePos?, lineMap: List<Int>, lang: Lang): String {
        val line = lineOf(pos, lineMap) ?: return ""
        return lang.pick("   (line $line)", "   (خط $line)")
    }

    private fun group(label: String, children: List<TreeNode>) = TreeNode(label, NodeKind.GROUP, children)

    private fun stmt(s: Stmt, lineMap: List<Int>, lang: Lang): TreeNode = when (s) {
        is Stmt.Assign -> TreeNode(
            lang.pick("Assign: ${s.name}", "انتساب: ${s.name}") + lineSuffix(s.pos, lineMap, lang),
            NodeKind.STATEMENT,
            listOf(expr(s.expr, lang))
        )
        is Stmt.Print -> TreeNode(
            lang.pick("Print", "چاپ") + lineSuffix(s.pos, lineMap, lang),
            NodeKind.STATEMENT,
            listOf(expr(s.expr, lang))
        )
        is Stmt.If -> {
            val kids = mutableListOf(
                group(lang.pick("condition", "شرط"), listOf(expr(s.condition, lang))),
                group(lang.pick("then", "آنگاه"), s.yes.map { stmt(it, lineMap, lang) })
            )
            if (s.no.isNotEmpty()) kids += group(lang.pick("else", "وگرنه"), s.no.map { stmt(it, lineMap, lang) })
            TreeNode(lang.pick("If", "اگر (If)") + lineSuffix(s.pos, lineMap, lang), NodeKind.STATEMENT, kids)
        }
        is Stmt.While -> TreeNode(
            lang.pick("While loop", "حلقه While") + lineSuffix(s.pos, lineMap, lang),
            NodeKind.STATEMENT,
            listOf(
                group(lang.pick("condition", "شرط"), listOf(expr(s.condition, lang))),
                group(lang.pick("body", "بدنه"), s.body.map { stmt(it, lineMap, lang) })
            )
        )
        else -> TreeNode(s.toString(), NodeKind.STATEMENT)
    }

    private fun number(value: Double, integer: Boolean): String =
        if (integer) value.toLong().toString() else value.toString()

    private fun expr(e: Expr, lang: Lang): TreeNode = when (e) {
        is Expr.NumberLit -> TreeNode(lang.pick("Number  ", "عدد  ") + number(e.value, e.integer), NodeKind.LITERAL)
        is Expr.StringLit -> TreeNode(lang.pick("Text  ", "متن  ") + "\"" + e.value + "\"", NodeKind.LITERAL)
        is Expr.BoolLit -> TreeNode(lang.pick("Boolean  ", "منطقی  ") + e.value, NodeKind.LITERAL)
        is Expr.Variable -> TreeNode(lang.pick("Variable  ", "متغیر  ") + e.name, NodeKind.VARIABLE)
        is Expr.Unary -> TreeNode(lang.pick("Unary  ", "یگانی  ") + e.op, NodeKind.OPERATOR, listOf(expr(e.expr, lang)))
        is Expr.Binary -> TreeNode(
            lang.pick("Operator  ", "عملگر  ") + e.op,
            NodeKind.OPERATOR,
            listOf(expr(e.left, lang), expr(e.right, lang))
        )
        else -> TreeNode(e.toString(), NodeKind.LITERAL)
    }

    /** Flatten to drawable rows using box-drawing characters. */
    fun flatten(root: TreeNode): List<TreeLine> {
        val out = mutableListOf<TreeLine>()
        out += TreeLine("", root.label, root.kind)
        fun walk(node: TreeNode, indent: String) {
            node.children.forEachIndexed { i, child ->
                val last = i == node.children.lastIndex
                out += TreeLine(indent + if (last) "└── " else "├── ", child.label, child.kind)
                walk(child, indent + if (last) "    " else "│   ")
            }
        }
        walk(root, "")
        return out
    }

    fun renderText(root: TreeNode): String = flatten(root).joinToString("\n") { it.prefix + it.label }

    fun nodeCount(root: TreeNode): Int = 1 + root.children.sumOf { nodeCount(it) }
}
