package com.mahdi.compilerstudio

/** UI language. Persisted as an Int (0 = English, 1 = Persian) so it is trivially saveable. */
enum class Lang(val code: Int) {
    EN(0),
    FA(1);

    companion object {
        fun fromCode(code: Int): Lang = if (code == 1) FA else EN
    }
}

/** Pick the English or Persian text. */
fun Lang.pick(en: String, fa: String): String = if (this == Lang.FA) fa else en
