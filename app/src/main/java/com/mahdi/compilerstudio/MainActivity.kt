package com.mahdi.compilerstudio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CompilerStudioApp() }
    }
}

@Composable
private fun CompilerStudioApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("compiler_studio", Context.MODE_PRIVATE) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var langCode by rememberSaveable {
        mutableIntStateOf(prefs.getInt("lang", if (Locale.getDefault().language == "fa") 1 else 0))
    }
    var sizeLevel by rememberSaveable { mutableIntStateOf(prefs.getInt("size", 1)) }
    var showWelcome by rememberSaveable { mutableStateOf(!prefs.getBoolean("welcome_seen", false)) }
    var source by rememberSaveable { mutableStateOf(prefs.getString("source", null) ?: Examples.DEFAULT_CODE) }
    var selectedStage by rememberSaveable { mutableIntStateOf(0) }
    var showSyntax by rememberSaveable { mutableStateOf(false) }
    var showLowered by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(true) }

    var outcome by remember { mutableStateOf<Pipeline.Outcome?>(null) }
    var ranSource by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var editRevision by remember { mutableIntStateOf(0) }
    var currentExample by remember { mutableStateOf<Example?>(null) }

    val lang = Lang.fromCode(langCode)
    val codeSize = when (sizeLevel) {
        0 -> 11
        2 -> 15
        else -> 13
    }
    val errors = remember(outcome, lang) {
        outcome?.let { ErrorExplainer.forOutcome(it, ranSource, lang) } ?: emptyList()
    }
    val treeRoot = remember(outcome, lang) {
        val o = outcome
        val ast = o?.result?.ast
        if (o != null && ast != null) AstTree.build(ast, o.lineMap, lang) else null
    }

    fun loadSource(text: String, example: Example? = null) {
        source = text
        currentExample = example
        editRevision++
        outcome = null
        selectedStage = 0
    }

    fun runNow() {
        if (isRunning) return
        val snapshot = source
        val revision = editRevision
        isRunning = true
        prefs.edit().putString("source", snapshot).apply()
        scope.launch {
            val result = withContext(Dispatchers.Default) { Pipeline.run(snapshot) }
            if (editRevision == revision) {
                outcome = result
                ranSource = snapshot
                val failed = result.result?.failedPhase
                selectedStage = if (failed != null) (failed - 1).coerceIn(0, 5) else 0
            }
            isRunning = false
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides if (lang == Lang.FA) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = Panel)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Bg)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 15.dp)
            ) {
                Header(
                    lang = lang,
                    onLang = {
                        val next = if (lang == Lang.FA) Lang.EN else Lang.FA
                        langCode = next.code
                        prefs.edit().putInt("lang", next.code).apply()
                    },
                    onSize = {
                        sizeLevel = (sizeLevel + 1) % 3
                        prefs.edit().putInt("size", sizeLevel).apply()
                    },
                    onHelp = { showWelcome = true }
                )

                if (showWelcome) {
                    WelcomeCard(lang) {
                        showWelcome = false
                        prefs.edit().putBoolean("welcome_seen", true).apply()
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ---- examples
                SectionLabel(lang.pick("EXAMPLES · tap one to load it", "نمونه‌ها · برای بارگذاری یکی را لمس کنید"))
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Examples.good.forEach { ex ->
                        val on = currentExample == ex
                        CsChip(ex.name(lang), color = if (on) Bg else MainText, background = if (on) Accent else PanelRaised) {
                            loadSource(ex.code, ex)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                SectionLabel(lang.pick("TRY A MISTAKE · see how errors are explained", "یک اشتباه را امتحان کنید · ببینید خطاها چطور توضیح داده می‌شوند"))
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Examples.mistakes.forEach { ex ->
                        val on = currentExample == ex
                        CsChip(ex.name(lang), color = if (on) Bg else Amber, background = if (on) Amber else PanelRaised) {
                            loadSource(ex.code, ex)
                        }
                    }
                }
                currentExample?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(text = it.description(lang), color = Muted, fontSize = 12.sp)
                }

                // ---- editor
                Spacer(Modifier.height(14.dp))
                val lineCount = source.lines().size
                SectionLabel(lang.pick("YOUR CODE · $lineCount LINES", "کد شما · $lineCount خط"))
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    CsChip(lang.pick("Syntax guide", "راهنمای نحو"), color = Accent) { showSyntax = true }
                    CsChip(lang.pick("Copy", "کپی")) { clipboard.setText(AnnotatedString(source)) }
                    CsChip(lang.pick("Paste", "چسباندن")) {
                        val pasted = clipboard.getText()?.text
                        if (!pasted.isNullOrEmpty()) loadSource(pasted)
                    }
                    CsChip(lang.pick("Clear", "پاک کردن")) { loadSource("") }
                    val lowered = outcome?.loweredSource
                    if (lowered != null && lowered != ranSource) {
                        CsChip(lang.pick("Lowered code", "کد تبدیل‌شده"), color = Green) { showLowered = true }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Ltr {
                    OutlinedTextField(
                        value = source,
                        onValueChange = {
                            source = it
                            currentExample = null
                            editRevision++
                            outcome = null
                            selectedStage = 0
                        },
                        modifier = Modifier.fillMaxWidth().height(230.dp),
                        textStyle = LocalTextStyle.current.copy(
                            color = MainText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = codeSize.sp,
                            lineHeight = (codeSize * 1.5f).sp
                        ),
                        placeholder = {
                            Text(
                                text = lang.pick("Write your program here, or load an example above.", "برنامه‌تان را اینجا بنویسید یا یک نمونه را بارگذاری کنید."),
                                color = Muted,
                                fontSize = 12.sp
                            )
                        },
                        shape = RoundedCornerShape(13.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Color(0xFF2B3850),
                            cursorColor = Accent,
                            focusedContainerColor = Panel,
                            unfocusedContainerColor = Panel
                        )
                    )
                }

                // ---- run
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { runNow() },
                    enabled = !isRunning,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Bg,
                        disabledContainerColor = Accent.copy(alpha = 0.55f)
                    )
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Bg, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(text = lang.pick("COMPILING...", "در حال کامپایل..."), fontWeight = FontWeight.Bold)
                    } else {
                        Text(text = lang.pick("▶  RUN COMPILER", "▶  اجرای کامپایلر"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                // ---- friendly errors
                if (errors.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    errors.take(5).forEach { error ->
                        ErrorCard(error, lang, codeSize) { stage -> selectedStage = stage }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // ---- pipeline
                Spacer(Modifier.height(14.dp))
                val result = outcome?.result
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(lang.pick("COMPILER PIPELINE · 6 STAGES", "مراحل کامپایلر · ۶ مرحله"))
                    Spacer(Modifier.weight(1f))
                    val status = when {
                        isRunning -> lang.pick("RUNNING", "در حال اجرا")
                        outcome == null -> lang.pick("READY", "آماده")
                        errors.isNotEmpty() -> lang.pick("ERROR", "خطا")
                        else -> lang.pick("ALL 6 DONE", "هر ۶ مرحله کامل")
                    }
                    val statusColor = when {
                        isRunning -> Amber
                        outcome == null -> Muted
                        errors.isNotEmpty() -> Red
                        else -> Green
                    }
                    Text(text = status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 0 until StageInfo.COUNT) {
                        val active = selectedStage == i
                        val failed = result != null && result.failedPhase == i + 1
                        val done = result != null && i < result.completedPhase
                        val mark = if (failed) "✗" else if (done) "✓" else "·"
                        val markColor = if (active) Bg else if (failed) Red else if (done) Green else Muted
                        val shape = RoundedCornerShape(10.dp)
                        Column(
                            Modifier
                                .width(80.dp)
                                .clip(shape)
                                .background(if (active) Accent else Panel)
                                .border(1.dp, if (failed) Red else Color.Transparent, shape)
                                .clickable { selectedStage = i }
                                .padding(vertical = 8.dp, horizontal = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "${i + 1}  $mark", color = markColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = StageInfo.tab(i, lang), color = if (active) Bg else MainText, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(Panel)
                        .border(1.dp, Color(0xFF25334A), RoundedCornerShape(13.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = StageInfo.title(selectedStage, lang),
                        color = MainText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAbout = !showAbout }) {
                        Text(
                            text = if (showAbout) lang.pick("Hide explanation", "پنهان کردن توضیح") else lang.pick("What is this stage?", "این مرحله چیست؟"),
                            color = Accent,
                            fontSize = 12.sp
                        )
                    }
                    if (showAbout) {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PanelRaised).padding(10.dp)
                        ) {
                            Text(text = StageInfo.about(selectedStage, lang), color = MainText, fontSize = 13.sp, lineHeight = 20.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(text = StageInfo.tryThis(selectedStage, lang), color = Amber, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    StageBody(outcome, ranSource, treeRoot, errors, selectedStage, lang, codeSize)
                }

                // ---- output
                Spacer(Modifier.height(12.dp))
                OutputPanel(
                    outcome = outcome,
                    hasError = errors.isNotEmpty(),
                    lang = lang,
                    codeSize = codeSize,
                    onCopy = { text -> clipboard.setText(AnnotatedString(text)) },
                    onShare = {
                        val report = Report.build(if (outcome == null) source else ranSource, outcome, lang)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "CompileLab")
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        context.startActivity(Intent.createChooser(intent, lang.pick("Share report", "اشتراک‌گذاری گزارش")))
                    }
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = lang.pick(
                        "Python-like educational compiler · v$APP_VERSION · runs fully offline",
                        "کامپایلر آموزشی شبه‌پایتون · نسخه $APP_VERSION · کاملاً آفلاین"
                    ),
                    color = Muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 18.dp)
                )
            }

            if (showLowered) {
                AlertDialog(
                    onDismissRequest = { showLowered = false },
                    containerColor = Panel,
                    titleContentColor = MainText,
                    textContentColor = MainText,
                    title = { Text(lang.pick("Lowered code (what the engine really reads)", "کد تبدیل‌شده (آنچه موتور واقعاً می‌خواند)")) },
                    text = {
                        Ltr {
                            Text(
                                text = outcome?.loweredSource.orEmpty(),
                                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(outcome?.loweredSource.orEmpty()))
                            showLowered = false
                        }) { Text(lang.pick("COPY", "کپی")) }
                    },
                    dismissButton = { TextButton(onClick = { showLowered = false }) { Text(lang.pick("CLOSE", "بستن")) } }
                )
            }
            if (showSyntax) {
                AlertDialog(
                    onDismissRequest = { showSyntax = false },
                    containerColor = Panel,
                    titleContentColor = MainText,
                    textContentColor = MainText,
                    title = { Text(lang.pick("Supported language syntax", "نحو پشتیبانی‌شده")) },
                    text = { SyntaxGuide(lang) },
                    confirmButton = { TextButton(onClick = { showSyntax = false }) { Text(lang.pick("CLOSE", "بستن")) } }
                )
            }
        }
    }
}

@Composable
private fun Header(lang: Lang, onLang: () -> Unit, onSize: () -> Unit, onHelp: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.compilerlab3),
            contentDescription = "CompileLab",
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(text = "CompileLab", color = MainText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = lang.pick("See how a compiler works, step by step", "ببینید کامپایلر قدم‌به‌قدم چطور کار می‌کند"),
                color = Muted,
                fontSize = 11.sp
            )
        }
        CsChip("Aa", onClick = onSize)
        Spacer(Modifier.width(6.dp))
        CsChip("?", color = Accent, onClick = onHelp)
        Spacer(Modifier.width(6.dp))
        CsChip(lang.pick("فارسی", "English"), color = Bg, background = Accent, onClick = onLang)
    }
}
@Composable
private fun WelcomeCard(lang: Lang, onDismiss: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(PanelRaised)
            .border(1.dp, Accent, RoundedCornerShape(13.dp))
            .padding(14.dp)
    ) {
        Text(
            text = lang.pick("Welcome! Here is how it works", "خوش آمدید! کار با برنامه این‌طور است"),
            color = MainText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = lang.pick(
                "1.  Pick an example below, or type a tiny program of your own.\n" +
                    "2.  Press ▶ RUN COMPILER.\n" +
                    "3.  Tap the six stages (Lexer → Bytecode) to see what the compiler did at each step: a table of tokens, a tree, tables of instructions.\n" +
                    "4.  If something is wrong, a card tells you in plain words what happened and how to fix it.\n\n" +
                    "Tip: use the button at the top to switch between English and Persian, and “Aa” to change the code size.",
                "۱.  یکی از نمونه‌های پایین را انتخاب کنید یا یک برنامه کوچک خودتان بنویسید.\n" +
                    "۲.  دکمه ▶ اجرای کامپایلر را بزنید.\n" +
                    "۳.  شش مرحله (واژه‌یاب ← بایت‌کد) را لمس کنید تا ببینید کامپایلر در هر مرحله چه کرده: جدول توکن‌ها، درخت، جدول دستورها.\n" +
                    "۴.  اگر مشکلی باشد، یک کارت با زبان ساده می‌گوید چه شده و چطور درست شود.\n\n" +
                    "نکته: با دکمه بالا بین فارسی و انگلیسی جابه‌جا شوید و با «Aa» اندازه کد را عوض کنید."
            ),
            color = MainText,
            fontSize = 13.sp,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onDismiss) {
            Text(text = lang.pick("Got it", "متوجه شدم"), color = Accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SyntaxGuide(lang: Lang) {
    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
        Text(
            text = lang.pick(
                "This compiler accepts a small Python-like language. It is not the full Python. Indent blocks with 4 spaces.",
                "این کامپایلر یک زبان کوچک شبه‌پایتون را می‌پذیرد، نه پایتون کامل. بلاک‌ها را با ۴ فاصله تو بدهید."
            ),
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(text = lang.pick("You can use", "می‌توانید استفاده کنید"), fontWeight = FontWeight.Bold, color = Green)
        Ltr {
            Text(
                text = "total = 12            # variable\nprint(total)          # output\nif x > 0:  /  elif  /  else:\nwhile x < 5:\nfor i in range(1, 6):\ndef add(a, b):  +  return a + b\nand   or   not   True   False",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = lang.pick(
                "Types: integer, decimal, text, boolean.\nOperators: + - * / %  and  == != < > <= >=.\nrange() takes 1–3 whole-number literals (negative step allowed).\nFunctions: one final return; call them as statements, in assignments, or inside print().\nText can contain Persian.",
                "نوع‌ها: عدد صحیح، اعشاری، متن، منطقی.\nعملگرها: + - * / %  و  == != < > <= >=.\nrange() یک تا سه عدد صحیح ثابت می‌گیرد (گام منفی هم مجاز است).\nتوابع: فقط یک return پایانی؛ به‌صورت دستور، در انتساب یا داخل print() صدا زده می‌شوند.\nمتن می‌تواند فارسی باشد."
            ),
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(text = lang.pick("Not available", "پشتیبانی نمی‌شود"), fontWeight = FontWeight.Bold, color = Amber)
        Text(
            text = lang.pick(
                "Recursion, functions calling other functions, several or conditional returns, default arguments, lists and dictionaries, for over a list, import, classes, input(), break / continue. Tabs and spaces cannot be mixed. Functions are expanded before compiling instead of being called at run time.",
                "بازگشتی، صدا زدن تابع از داخل تابع، چند return یا return شرطی، آرگومان پیش‌فرض، لیست و دیکشنری، for روی لیست، import، کلاس، input()، break / continue. Tab و فاصله را با هم مخلوط نکنید. توابع پیش از کامپایل باز می‌شوند و در زمان اجرا صدا زده نمی‌شوند."
            ),
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(text = text, color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
}

@Composable
private fun TableOrEmpty(table: Table, lang: Lang, codeSize: Int) {
    if (table.rows.isEmpty()) EmptyNote(lang.pick("(nothing to show)", "(چیزی برای نمایش نیست)"))
    else TableView(table, codeSize)
}

/** The content of the selected stage: table, tree, or an explanation of why there is none. */
@Composable
private fun StageBody(
    outcome: Pipeline.Outcome?,
    ranSource: String,
    treeRoot: TreeNode?,
    errors: List<FriendlyError>,
    stage: Int,
    lang: Lang,
    codeSize: Int
) {
    val r = outcome?.result
    if (outcome == null) {
        EmptyNote(
            lang.pick(
                "Nothing to show yet. Press ▶ RUN COMPILER and this area will fill with the result of this stage.",
                "هنوز چیزی برای نمایش نیست. ▶ اجرای کامپایلر را بزنید تا نتیجه این مرحله اینجا بیاید."
            )
        )
        return
    }
    if (r == null) {
        EmptyNote(
            lang.pick(
                "Your code could not even be read, so no stage ran. Read the red card above.",
                "کد شما حتی خوانده نشد، پس هیچ مرحله‌ای اجرا نشد. کارت قرمز بالا را بخوانید."
            )
        )
        return
    }
    val failedStage = r.failedPhase
    val reached = stage < r.completedPhase || failedStage == stage + 1
    if (!reached) {
        EmptyNote(
            lang.pick(
                "This stage did not run because stage ${failedStage ?: "?"} failed first.",
                "این مرحله اجرا نشد چون مرحله ${failedStage ?: "؟"} پیش‌تر با خطا مواجه شد."
            )
        )
        return
    }
    Column(Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
        val rewritten = outcome.loweredSource != null && outcome.loweredSource != ranSource
        errors.filter { it.stage == stage + 1 }.forEach { e ->
            val where = e.line?.let { lang.pick("Line $it: ", "خط $it: ") } ?: ""
            Text(text = "✗ " + where + e.title, color = Red, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 19.sp)
            Spacer(Modifier.height(8.dp))
        }
        when (stage) {
            0 -> {
                if (rewritten) {
                    EmptyNote(
                        lang.pick(
                            "Note: the lexer reads your code after it was rewritten into the engine's own syntax (see “Lowered code”), so you may also see { } ; symbols or helper names like __cs_fn_1.",
                            "توجه: واژه‌یاب کد شما را پس از تبدیل به نحو داخلی موتور می‌خواند (به «کد تبدیل‌شده» نگاه کنید)، پس ممکن است نمادهای { } ; یا نام‌های کمکی مثل __cs_fn_1 را هم ببینید."
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                }
                val tokenTable = StageTables.tokens(r.tokens, outcome.lineMap, lang)
                val lexError = errors.firstOrNull { it.stage == 1 }
                val shownTable = if (lexError == null) tokenTable else tokenTable.copy(
                    rows = tokenTable.rows + listOf(
                        listOf("✗", lexError.line?.toString() ?: "?", lang.pick("ERROR", "خطا"), "⚠", lexError.title)
                    )
                )
                TableOrEmpty(shownTable, lang, codeSize)
                if (r.tokens.size > 400) {
                    Spacer(Modifier.height(6.dp))
                    EmptyNote(lang.pick("Showing the first 400 of ${r.tokens.size} tokens.", "۴۰۰ توکن اول از ${r.tokens.size} توکن نمایش داده شد."))
                }
            }
            1 -> {
                if (treeRoot == null) {
                    EmptyNote(
                        lang.pick(
                            "No tree was built because the Parser stopped at an error (see the red card).",
                            "درختی ساخته نشد چون تجزیه‌گر به خطا برخورد کرد (کارت قرمز را ببینید)."
                        )
                    )
                } else {
                    EmptyNote(
                        lang.pick(
                            "Read it top-down: each branch is a part of the line above it. Deeper = calculated first.",
                            "از بالا به پایین بخوانید: هر شاخه بخشی از خط بالای خودش است. هرچه عمیق‌تر باشد زودتر محاسبه می‌شود."
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    TreeView(AstTree.flatten(treeRoot), codeSize)
                }
            }
            2 -> {
                if (rewritten) {
                    EmptyNote(
                        lang.pick(
                            "Names starting with __cs_fn come from function expansion.",
                            "نام‌هایی که با __cs_fn شروع می‌شوند از باز شدن توابع می‌آیند."
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (r.symbols.isEmpty()) EmptyNote(lang.pick("No variables were created.", "هیچ متغیری ساخته نشد."))
                else TableView(StageTables.symbols(r.symbols, outcome.lineMap, lang), codeSize)
            }
            3 -> {
                EmptyNote(lang.pick("${r.tac.size} small steps.", "${r.tac.size} گام کوچک."))
                Spacer(Modifier.height(8.dp))
                TableOrEmpty(StageTables.tac(r.tac, lang), lang, codeSize)
            }
            4 -> {
                val before = r.tac.size
                val after = r.optimizedTac.size
                EmptyNote(
                    lang.pick(
                        "Before: $before steps → after: $after steps" + if (before > after) " (${before - after} removed)." else ".",
                        "قبل: $before گام ← بعد: $after گام" + if (before > after) " (${before - after} گام حذف شد)." else "."
                    )
                )
                Spacer(Modifier.height(8.dp))
                TableOrEmpty(StageTables.tac(r.optimizedTac, lang), lang, codeSize)
            }
            else -> {
                EmptyNote(lang.pick("${r.bytecode.size} machine instructions.", "${r.bytecode.size} دستور ماشین."))
                Spacer(Modifier.height(8.dp))
                TableOrEmpty(StageTables.bytecode(r.bytecode, lang), lang, codeSize)
            }
        }
    }
}

@Composable
private fun OutputPanel(
    outcome: Pipeline.Outcome?,
    hasError: Boolean,
    lang: Lang,
    codeSize: Int,
    onCopy: (String) -> Unit,
    onShare: () -> Unit
) {
    val r = outcome?.result
    val output = r?.output.orEmpty()
    val text = when {
        outcome == null -> lang.pick("The output of your program appears here after you press RUN.", "خروجی برنامه شما بعد از زدن اجرا اینجا نمایش داده می‌شود.")
        r == null -> lang.pick("No output: fix the error above first.", "خروجی نیست: ابتدا خطای بالا را رفع کنید.")
        output.isBlank() -> lang.pick("No output: the program stopped because of the error above.", "خروجی نیست: برنامه به‌خاطر خطای بالا متوقف شد.")
        else -> output
    }
    val color = when {
        outcome == null -> Muted
        hasError -> Red
        output.isNotBlank() -> Green
        else -> Muted
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xFF10192A))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(lang.pick("PROGRAM OUTPUT", "خروجی برنامه"))
            Spacer(Modifier.weight(1f))
            if (outcome != null && output.isNotBlank()) {
                TextButton(onClick = { onCopy(output) }) { Text(text = lang.pick("COPY", "کپی"), color = Accent, fontSize = 11.sp) }
            }
            TextButton(onClick = onShare) { Text(text = lang.pick("SHARE REPORT", "اشتراک گزارش"), color = Accent, fontSize = 11.sp) }
        }
        Spacer(Modifier.height(4.dp))
        if (outcome != null && !hasError && output.isNotBlank()) {
            Ltr {
                Text(text = text, color = color, fontFamily = FontFamily.Monospace, fontSize = codeSize.sp, lineHeight = (codeSize * 1.4f).sp)
            }
        } else {
            Text(text = text, color = color, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}
