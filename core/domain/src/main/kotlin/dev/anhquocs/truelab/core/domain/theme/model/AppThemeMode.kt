package dev.anhquocs.truelab.core.domain.theme.model

enum class AppThemeMode(val key: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");

    companion object {
        fun fromKey(key: String): AppThemeMode {
            return AppThemeMode.entries.firstOrNull { it.key == key } ?: SYSTEM
        }
    }
}
