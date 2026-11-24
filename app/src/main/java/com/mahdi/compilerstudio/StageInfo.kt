package com.mahdi.compilerstudio

/** Plain-language descriptions of the six compiler stages (index 0..5). */
object StageInfo {
    const val COUNT = 6

    /** Short tab name. */
    fun tab(i: Int, lang: Lang): String = when (i) {
        0 -> lang.pick("Lexer", "واژه‌یاب")
        1 -> lang.pick("Parser", "تجزیه")
        2 -> lang.pick("Semantic", "معنایی")
        3 -> lang.pick("IR / TAC", "کد میانی")
        4 -> lang.pick("Optimize", "بهینه‌سازی")
        else -> lang.pick("Bytecode", "بایت‌کد")
    }

    fun title(i: Int, lang: Lang): String = when (i) {
        0 -> lang.pick("1 · Lexer – cut the code into tokens", "۱ · واژه‌یاب (Lexer) – برش کد به توکن")
        1 -> lang.pick("2 · Parser – build the syntax tree", "۲ · تجزیه‌گر (Parser) – ساخت درخت نحو")
        2 -> lang.pick("3 · Semantic analysis – check the meaning", "۳ · تحلیل معنایی – بررسی معنا")
        3 -> lang.pick("4 · Intermediate code (TAC)", "۴ · کد میانی (TAC)")
        4 -> lang.pick("5 · Optimization – make it simpler", "۵ · بهینه‌سازی – ساده‌تر کردن")
        else -> lang.pick("6 · Bytecode and virtual machine", "۶ · بایت‌کد و ماشین مجازی")
    }

    fun about(i: Int, lang: Lang): String = when (i) {
        0 -> lang.pick(
            "Like splitting a sentence into words. The lexer reads your text character by character and groups it into tokens: keywords (if, print), names, numbers, text and operators. It only checks that every character is allowed, not whether the sentence makes sense.",
            "مثل جدا کردن کلمات یک جمله. واژه‌یاب متن شما را حرف به حرف می‌خواند و به توکن تبدیل می‌کند: کلیدواژه‌ها (if، print)، نام‌ها، عددها، متن و عملگرها. فقط بررسی می‌کند که نویسه‌ها مجاز باشند، نه اینکه جمله معنا داشته باشد."
        )
        1 -> lang.pick(
            "Like analysing the grammar of a sentence. The parser arranges the tokens into a tree that shows what belongs to what, and in which order calculations happen (for example * before +). Grammar mistakes such as a missing parenthesis are found here.",
            "مثل تحلیل دستور زبان یک جمله. تجزیه‌گر توکن‌ها را در یک درخت می‌چیند که نشان می‌دهد چه چیزی به چه چیزی تعلق دارد و محاسبات به چه ترتیبی انجام می‌شوند (مثلاً * قبل از +). اشتباه‌های دستوری مثل پرانتز جاافتاده اینجا پیدا می‌شوند."
        )
        2 -> lang.pick(
            "Grammar can be right while the meaning is wrong. This stage keeps a table of your variables and their types, and checks things like: is the variable defined, can you really add these two values, is the condition true/false.",
            "دستور زبان ممکن است درست باشد ولی معنا غلط. این مرحله جدولی از متغیرها و نوع آن‌ها نگه می‌دارد و مواردی مثل این را بررسی می‌کند: متغیر تعریف شده؟ آیا می‌شود این دو مقدار را جمع کرد؟ آیا شرط درست/نادرست است؟"
        )
        3 -> lang.pick(
            "The tree is rewritten as very small steps, each doing one thing (three-address code: result = a op b). Temporary names t1, t2 ... hold in-between results and labels L1, L2 ... mark places to jump to.",
            "درخت به گام‌های بسیار کوچک بازنویسی می‌شود که هر کدام یک کار انجام می‌دهد (کد سه‌آدرسه: نتیجه = a عملگر b). نام‌های موقت t1، t2 ... نتایج میانی را نگه می‌دارند و برچسب‌های L1، L2 ... مقصد پرش‌ها را نشان می‌دهند."
        )
        4 -> lang.pick(
            "The compiler makes the steps smaller or faster without changing the result. For example 2 * 3 is calculated once now (constant folding) instead of every time the program runs. Compare the instruction count with the previous stage.",
            "کامپایلر گام‌ها را بدون تغییر نتیجه کوچک‌تر یا سریع‌تر می‌کند. مثلاً 2 * 3 همین حالا یک بار حساب می‌شود (ثابت‌یابی) و دیگر هر بار اجرای برنامه حساب نمی‌شود. تعداد دستورها را با مرحله قبل مقایسه کنید."
        )
        else -> lang.pick(
            "The optimized steps become bytecode: instructions for a tiny virtual computer that uses a stack (LOAD pushes a value, ADD pops two and pushes the sum...). The virtual machine runs them one by one, and what PRINT shows is the program output. A safety limit stops endless loops.",
            "گام‌های بهینه‌شده به بایت‌کد تبدیل می‌شوند: دستورهایی برای یک کامپیوتر مجازی کوچک که از پشته استفاده می‌کند (LOAD یک مقدار را می‌گذارد، ADD دو مقدار را برداشته و مجموع را می‌گذارد ...). ماشین مجازی آن‌ها را یکی‌یکی اجرا می‌کند و خروجی PRINT همان خروجی برنامه است. یک حد ایمنی جلوی حلقه‌های بی‌پایان را می‌گیرد."
        )
    }

    fun tryThis(i: Int, lang: Lang): String = when (i) {
        0 -> lang.pick("Try it: type a character like @ and run again.", "امتحان کنید: یک نویسه مثل @ بنویسید و دوباره اجرا کنید.")
        1 -> lang.pick("Try it: delete a closing ) and run again.", "امتحان کنید: یک ) پایانی را پاک کنید و دوباره اجرا کنید.")
        2 -> lang.pick("Try it: use a variable you never created.", "امتحان کنید: از متغیری استفاده کنید که نساخته‌اید.")
        3 -> lang.pick("Try it: change a calculation and watch the steps change.", "امتحان کنید: یک محاسبه را عوض کنید و تغییر گام‌ها را ببینید.")
        4 -> lang.pick("Try it: write x = 2 * 3 + 4 and compare both stages.", "امتحان کنید: بنویسید x = 2 * 3 + 4 و دو مرحله را مقایسه کنید.")
        else -> lang.pick("Try it: write an endless while loop to see the safety limit.", "امتحان کنید: یک حلقه while بی‌پایان بنویسید تا حد ایمنی را ببینید.")
    }
}
