package com.ainotes.app.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.ainotes.app.domain.model.AiAction
import com.ainotes.app.settings.ThemeMode

data class AppLang(val lang: String)

val LocalLang = staticCompositionLocalOf { AppLang("fa") }

/** Picks the Persian or English variant of a UI string. */
@Composable
fun txt(fa: String, en: String): String =
    if (LocalLang.current.lang == "en") en else fa

@Composable
fun actionTxt(action: AiAction): String = when (action) {
    AiAction.ORGANIZE -> txt("سازمان‌دهی", "Organize")
    AiAction.SUMMARIZE -> txt("خلاصه", "Summarize")
    AiAction.EXPAND -> txt("گسترش", "Expand")
    AiAction.REWRITE -> txt("بازنویسی", "Rewrite")
    AiAction.EXTRACT_TASKS -> txt("استخراج وظایف", "Extract Tasks")
    AiAction.GENERATE_TITLE -> txt("تولید عنوان", "Generate Title")
    AiAction.TRANSLATE -> txt("ترجمه", "Translate")
    AiAction.ASK -> txt("پرسش از AI", "Ask AI")
    AiAction.MASTERPIECE -> txt("✨ مسترپیس", "✨ Masterpiece")
}

@Composable
fun themeTxt(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> txt("سیستم", "System")
    ThemeMode.LIGHT -> txt("روشن", "Light")
    ThemeMode.DARK -> txt("تیره", "Dark")
}

@Composable
fun stageTxt(key: String): String = when (key) {
    "analyzing" -> txt("تحلیل محتوا", "Analyzing content")
    "sections" -> txt("ساخت بخش‌ها", "Creating sections")
    "formatting" -> txt("بهبود قالب‌بندی", "Improving formatting")
    else -> key
}

/** Translates known error keys; passes technical/network messages through. */
fun errorMessage(raw: String, lang: String): String {
    val en = lang == "en"
    return when {
        raw == "err:emptyNote" -> if (en) "The note is empty" else "یادداشت خالیه"
        raw == "err:noProvider" ->
            if (en) "No AI provider configured - add one in Settings."
            else "هیچ سرویس AI تنظیم نشده؛ از تنظیمات یکی اضافه کن."
        raw == "err:aiFailed" -> if (en) "AI request failed" else "درخواست AI ناموفق بود"
        raw.startsWith("err:saveFailed:") ->
            (if (en) "Save failed: " else "ذخیره انجام نشد: ") + raw.removePrefix("err:saveFailed:")
        else -> raw
    }
}
