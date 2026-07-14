package com.kyrx.mypresence.domain.model

import androidx.compose.runtime.Stable

@Stable
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val versionName: String = "",
    val versionCode: Long = 0,
    val category: AppCategory = AppCategory.OTHER,
    val isFavorite: Boolean = false
)

enum class AppCategory(val displayName: String, val keywords: List<String> = emptyList()) {
    GAMES("Games", listOf("game", "gaming")),
    MUSIC("Music", listOf("music", "audio", "spotify", "soundcloud", "apple music")),
    VIDEO("Video", listOf("video", "youtube", "netflix", "prime", "disney", "hotstar", "vlc", "mx player", "media")),
    SOCIAL("Social", listOf("social", "instagram", "facebook", "twitter", "discord", "reddit", "telegram", "whatsapp", "messenger", "signal", "snapchat", "linkedin", "tiktok", "threads")),
    MESSAGING("Messaging", listOf("messaging", "chat", "sms", "message", "whatsapp", "telegram", "signal", "line", "wechat")),
    BROWSER("Browser", listOf("browser", "chrome", "firefox", "edge", "opera", "samsung internet", "webview", "kiwi")),
    DEVELOPMENT("Development", listOf("dev", "code", "studio", "terminal", "vscode", "android studio", "github", "git")),
    PRODUCTIVITY("Productivity", listOf("productivity", "office", "docs", "sheets", "slides", "drive", "notes", "calendar", "word", "excel", "powerpoint", "pdf", "adobe")),
    READING("Reading", listOf("book", "read", "kindle", "comic", "manga", "pdf", "reader", "news", "medium")),
    SHOPPING("Shopping", listOf("shop", "amazon", "flipkart", "myntra", "ebay", "walmart", "aliexpress")),
    FINANCE("Finance", listOf("finance", "bank", "pay", "gpay", "phonepe", "paytm", "credit", "wallet", "upi")),
    TRAVEL("Travel", listOf("travel", "uber", "ola", "maps", "navigation", "gps", "flight", "booking", "zomato", "swiggy")),
    EDUCATION("Education", listOf("edu", "learn", "course", "school", "college", "classroom", "duolingo", "khan", "udemy")),
    ENTERTAINMENT("Entertainment", listOf("entertainment", "tv", "theater", "cinema", "watch", "stream", "live")),
    TOOLS("Tools", listOf("tool", "utility", "file", "manager", "cleaner", "launcher", "keyboard", "clock", "calculator", "weather")),
    SYSTEM("System", listOf("android", "system", "google", "com.android", "com.google", "settings", "service")),
    OTHER("Other");

    companion object {
        private val allKeywords = entries.flatMap { cat -> cat.keywords.map { it.lowercase() to cat } }

        fun categorize(packageName: String, appName: String, isSystemApp: Boolean): AppCategory {
            if (isSystemApp) return SYSTEM
            val lowerName = appName.lowercase()
            val lowerPkg = packageName.lowercase()
            val matches = allKeywords.filter { (kw) -> lowerName.contains(kw) || lowerPkg.contains(kw) }
            if (matches.isNotEmpty()) return matches.first().second
            val pkgSegments = lowerPkg.split(".")
            if (pkgSegments.size >= 3) {
                val base = pkgSegments[1]
                if (base in listOf("google", "android")) return SYSTEM
            }
            return OTHER
        }
    }
}
