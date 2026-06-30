package moe.matsuri.nb4a.tproxy.utils

fun String.shellQuote(): String {
    return "'${replace("'", "'\"'\"'")}'"
}

fun String.shellQuoteForCase(): String {
    return replace("\\", "\\\\")
        .replace("'", "'\"'\"'")
        .replace("*", "\\*")
        .replace("?", "\\?")
        .replace("[", "\\[")
}
