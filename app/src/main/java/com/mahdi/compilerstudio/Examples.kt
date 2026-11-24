package com.mahdi.compilerstudio

data class Example(
    val nameEn: String,
    val nameFa: String,
    val descEn: String,
    val descFa: String,
    val code: String,
    /** true = the program contains a deliberate mistake, to show how errors are reported. */
    val isMistake: Boolean = false
) {
    fun name(lang: Lang) = lang.pick(nameEn, nameFa)
    fun description(lang: Lang) = lang.pick(descEn, descFa)
}

object Examples {
    const val DEFAULT_CODE = "# Arithmetic and variables\nx = 7\ny = 5\ntotal = x * y + 2\nprint(total)\n"

    val good: List<Example> = listOf(
        Example(
            "Arithmetic", "محاسبات",
            "Variables and math. Prints 37.", "متغیر و محاسبه. عدد 37 چاپ می‌شود.",
            DEFAULT_CODE
        ),
        Example(
            "If / Else", "اگر / وگرنه",
            "Choose between two actions.", "انتخاب بین دو کار.",
            "# Python-like indentation\nscore = 82\nif score >= 60:\n    print(\"Passed\")\nelse:\n    print(\"Try again\")\n"
        ),
        Example(
            "While loop", "حلقه While",
            "Repeat while a condition is true.", "تکرار تا وقتی شرط درست است.",
            "# Indentation-based while loop\ncounter = 0\nwhile counter < 4:\n    print(counter)\n    counter = counter + 1\n"
        ),
        Example(
            "For / range", "حلقه For",
            "Add the numbers 1 to 5.", "جمع اعداد ۱ تا ۵.",
            "# for loop over a literal range\nsum = 0\nfor i in range(1, 6):\n    sum = sum + i\nprint(sum)\n"
        ),
        Example(
            "Nested + elif", "تو در تو + elif",
            "An if inside a loop, with several branches.", "if داخل حلقه، با چند شاخه.",
            "# Nested indentation and elif\nnumber = 1\nwhile number <= 5:\n    if number % 2 == 0:\n        print(\"even\")\n    elif number == 1:\n        print(\"first\")\n    else:\n        print(\"odd\")\n    number = number + 1\n"
        ),
        Example(
            "Functions", "توابع",
            "Define a function and call it twice.", "تعریف یک تابع و دوبار صدا زدن آن.",
            "# Function definitions, parameters and return\n\ndef calculate_total(price, count):\n    subtotal = price * count\n    return subtotal + 5\n\nfirst = calculate_total(12, 3)\nsecond = calculate_total(7, 2)\nprint(first)\nprint(second)\n"
        ),
        Example(
            "FizzBuzz", "فیزبازز",
            "Classic exercise with remainder (%).", "تمرین کلاسیک با باقیمانده (%).",
            "# Multiples of 3 and 5\nfor n in range(1, 16):\n    if n % 15 == 0:\n        print(\"FizzBuzz\")\n    elif n % 3 == 0:\n        print(\"Fizz\")\n    elif n % 5 == 0:\n        print(\"Buzz\")\n    else:\n        print(n)\n"
        ),
        Example(
            "Factorial", "فاکتوریل",
            "Multiply 1 * 2 * 3 * 4 * 5 with a loop. Prints 120.", "حاصل‌ضرب 1 تا 5 با حلقه. عدد 120 چاپ می‌شود.",
            "# 5! = 120\nresult = 1\nn = 5\nwhile n > 1:\n    result = result * n\n    n = n - 1\nprint(result)\n"
        ),
        Example(
            "Text", "متن",
            "Join pieces of text (also Persian).", "چسباندن تکه‌های متن (فارسی هم کار می‌کند).",
            "# Text can be joined with +\nname = \"Sara\"\nprint(\"Hello \" + name)\nprint(\"سلام \" + name)\n"
        )
    )

    val mistakes: List<Example> = listOf(
        Example(
            "Missing )", "پرانتز جاافتاده",
            "The Parser finds it.", "تجزیه‌گر آن را پیدا می‌کند.",
            "print(\"Hello\"\n", true
        ),
        Example(
            "Missing :", "دونقطه جاافتاده",
            "A block header without a colon.", "سرخط بلاک بدون دونقطه.",
            "x = 3\nif x > 1\n    print(\"big\")\n", true
        ),
        Example(
            "Unknown variable", "متغیر ناشناخته",
            "The Semantic stage finds it.", "مرحله معنایی آن را پیدا می‌کند.",
            "price = 10\nprint(prise * 2)\n", true
        ),
        Example(
            "Wrong type", "نوع اشتباه",
            "A number variable receives text.", "متغیر عددی متن می‌گیرد.",
            "age = 20\nage = \"twenty\"\n", true
        ),
        Example(
            "Bad character", "نویسه نامعتبر",
            "The Lexer finds it.", "واژه‌یاب آن را پیدا می‌کند.",
            "x = 5 @ 3\n", true
        ),
        Example(
            "Endless loop", "حلقه بی‌پایان",
            "The VM safety limit stops it.", "حد ایمنی ماشین مجازی آن را متوقف می‌کند.",
            "x = 1\nwhile x > 0:\n    x = x + 1\n", true
        )
    )
}
