package com.ainotes.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ainotes.app.AppContainer
import com.ainotes.app.domain.model.ExportFormat
import com.ainotes.app.settings.ThemeMode
import com.ainotes.app.ui.components.SectionLabel
import com.ainotes.app.ui.i18n.txt
import com.ainotes.app.ui.i18n.themeTxt
import com.ainotes.app.ui.theme.hexToColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ainotes.app.sync.CustomCloudConfig
import com.ainotes.app.sync.CustomCloudStorage

private val ACCENTS = listOf("#7C4DFF", "#00A0A8", "#E91E63", "#FF6D00", "#2E7D32", "#3D5AFE")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenProviders: () -> Unit
) {
    val themeMode by container.settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val accent by container.settings.accent.collectAsState(initial = "#7C4DFF")
    val defaultExport by container.settings.defaultExport.collectAsState(initial = "PDF")
    val customPrompt by container.settings.customPrompt.collectAsState(initial = "")
    val scope = rememberCoroutineScope()

    var showPrompt by remember { mutableStateOf(false) }
    var promptDraft by remember { mutableStateOf(customPrompt) }
    var showCloud by remember { mutableStateOf(false) }
    var showLang by remember { mutableStateOf(false) }
    val lang by container.settings.appLanguage.collectAsState(initial = "fa")
    var showBackup by remember { mutableStateOf(false) }
    var backingUp by remember { mutableStateOf(false) }
    var cloudStatus by remember { mutableStateOf<String?>(null) }
    var cloudUrl by remember {
        mutableStateOf(container.secureStore.get("cloud_url") ?: "")
    }
    var cloudKey by remember {
        mutableStateOf(if (container.secureStore.get("cloud_key") == null) "" else "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(txt("تنظیمات", "Settings")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionLabel(txt("ظاهر", "Appearance"))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { scope.launch { container.settings.setTheme(mode) } },
                        label = { Text(themeTxt(mode)) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ACCENTS.forEach { hex ->
                    Surface(
                        shape = CircleShape,
                        color = hexToColor(hex),
                        border = if (accent == hex) {
                            androidx.compose.foundation.BorderStroke(
                                3.dp,
                                MaterialTheme.colorScheme.onBackground
                            )
                        } else null,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(CircleShape)
                            .clickable { scope.launch { container.settings.setAccent(hex) } }
                    ) {
                        Box(Modifier.size(26.dp))
                    }
                }
            }

            SectionLabel(txt("هوش مصنوعی", "AI"))
            SettingRow(txt("سرویس‌های AI", "AI Providers"), txt("پیکربندی API دلخواه خودت", "Configure your own API endpoint")) { onOpenProviders() }
            SettingRow(txt("دستورالعمل AI", "AI Instructions"), txt("ویرایش پرامپت سازمان‌دهی", "Customize the organization prompt")) { showPrompt = true }
            SettingRow(
                txt("حریم خصوصی", "Privacy"),
                txt(
                    "متن یادداشت فقط وقتی از اکشن‌های AI استفاده کنی به سرور خودت فرستاده میشه",
                    "Note content is sent to your AI server only when you use AI actions"
                )
            ) { }

            SectionLabel(txt("خروجی", "Export"))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                ExportFormat.entries.forEach { format ->
                    FilterChip(
                        selected = defaultExport == format.name,
                        onClick = { scope.launch { container.settings.setDefaultExport(format.name) } },
                        label = { Text(format.extension.uppercase()) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            SectionLabel(txt("ذخیرهٔ ابری", "Cloud storage"))
            SettingRow(
                txt("سرور ذخیرهٔ ابری", "Custom storage server"),
                txt("آدرس سرور و کلید API برای پشتیبان‌گیری", "Server URL and API key for backup")
            ) { showCloud = true }
            SettingRow(
                txt("پشتیبان‌گیری اکنون", "Backup now"),
                if (backingUp) txt("در حال آپلود...", "Uploading...")
                else txt(
                    "همهٔ یادداشت‌ها رو به سرورت میفرسته — فقط با دستور خودت",
                    "Uploads all notes to your server - only on your command"
                )
            ) { showBackup = true }

            SettingRow(
                txt("زبان", "Language"),
                if (lang == "en") "English — left-to-right" else "فارسی — راست‌نویس"
            ) { showLang = true }

            SectionLabel(txt("درباره", "About"))
            SettingRow(txt("نسخهٔ اپ", "App version"), "1.0.0") { }
            SettingRow(txt("کتابخانه‌های متن‌باز", "Open-source licenses"), "Compose, Room, OkHttp, WorkManager") { }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showPrompt) {
        AlertDialog(
            onDismissRequest = { showPrompt = false },
            title = { Text(txt("دستورالعمل AI", "AI Instructions")) },
            text = {
                OutlinedTextField(
                    value = promptDraft,
                    onValueChange = { promptDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    placeholder = { Text(com.ainotes.app.ai.PromptManager.DEFAULT_SYSTEM_PROMPT) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { container.settings.setCustomPrompt(promptDraft) }
                    showPrompt = false
                }) { Text(txt("ذخیره", "Save")) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        promptDraft = com.ainotes.app.ai.PromptManager.DEFAULT_SYSTEM_PROMPT
                    }) { Text(txt("بازنشانی", "Reset")) }
                    TextButton(onClick = { showPrompt = false }) { Text(txt("انصراف", "Cancel")) }
                }
            }
        )
    }

    if (showBackup) {
        AlertDialog(
            onDismissRequest = { showBackup = false },
            title = { Text(txt("آپلود به سرورت؟", "Upload to your server?")) },
            text = {
                Text(
                    txt(
                        "یادداشت‌هات به این آدرس فرستاده میشه: " +
                            (cloudUrl.ifBlank { "(هنوز آدرس سروری تنظیم نشده)" }) +
                            ".\n\nآپلود فقط وقتی انجام میشه که خودت «شروع پشتیبان‌گیری» رو بزنی — هیچ‌چیز بی‌صدا فرستاده نمیشه.",
                        "Your notes will be sent to: " +
                            (cloudUrl.ifBlank { "(no server URL set yet)" }) +
                            ".\n\nUploading happens only when you tap Backup - nothing is sent silently."
                    )
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !backingUp,
                    onClick = {
                        showBackup = false
                        backingUp = true
                        cloudStatus = null
                        scope.launch(Dispatchers.IO) {
                            val url = container.secureStore.get("cloud_url") ?: ""
                            val key = container.secureStore.get("cloud_key") ?: ""
                            cloudStatus = if (url.isBlank()) {
                                txt("اول آدرس سرور رو تنظیم کن (تنظیمات ذخیرهٔ ابری).", "Set a server URL first (Cloud storage settings).")
                            } else {
                                val notes = runCatching { container.repository.allNotes() }
                                    .getOrDefault(emptyList())
                                val storage = CustomCloudStorage(CustomCloudConfig(url, key))
                                val ok = runCatching { storage.backup(notes) }.getOrDefault(false)
                                if (ok) txt("${notes.size} یادداشت با موفقیت آپلود شد.", "${notes.size} notes uploaded successfully.")
                                else txt("آپلود ناموفق بود — آدرس سرور و کلید API رو چک کن.", "Upload failed - check server URL and API key.")
                            }
                            backingUp = false
                        }
                    }
                ) { Text(txt("شروع پشتیبان‌گیری", "Start backup")) }
            },
            dismissButton = {
                TextButton(onClick = { showBackup = false }) { Text(txt("انصراف", "Cancel")) }
            }
        )
    }

    cloudStatus?.let { status ->
        AlertDialog(
            onDismissRequest = { cloudStatus = null },
            title = { Text(txt("نتیجهٔ پشتیبان‌گیری", "Backup result")) },
            text = { Text(status) },
            confirmButton = {
                TextButton(onClick = { cloudStatus = null }) { Text(txt("باشه", "OK")) }
            }
        )
    }

    if (showLang) {
        AlertDialog(
            onDismissRequest = { showLang = false },
            title = { Text(txt("زبان", "Language")) },
            text = {
                Column {
                    Text(
                        "فارسی — راست‌نویس",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (lang == "fa") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { container.settings.setLanguage("fa") }
                            }
                            .padding(vertical = 12.dp)
                    )
                    Text(
                        "English — left-to-right",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (lang == "en") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { container.settings.setLanguage("en") }
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLang = false }) { Text(txt("بستن", "Close")) }
            }
        )
    }

    if (showCloud) {
        AlertDialog(
            onDismissRequest = { showCloud = false },
            title = { Text(txt("ذخیره‌ساز ابری اختصاصی", "Custom cloud storage")) },
            text = {
                Column {
                    OutlinedTextField(
                        value = cloudUrl,
                        onValueChange = { cloudUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(txt("آدرس سرور، مثلاً https://example.com/api", "Server URL, e.g. https://example.com/api")) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cloudKey,
                        onValueChange = { cloudKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(txt("کلید API", "API key")) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        txt("به‌صورت رمزنگاری‌شده روی همین دستگاه ذخیره میشه و هیچ‌وقت وارد کد نمیشه.", "Stored encrypted on this device; never written into source code."),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    container.secureStore.put("cloud_url", cloudUrl)
                    if (cloudKey.isNotBlank() && !cloudKey.contains("\u2022")) {
                        container.secureStore.put("cloud_key", cloudKey)
                    }
                    showCloud = false
                }) { Text(txt("ذخیره", "Save")) }
            },
            dismissButton = {
                TextButton(onClick = { showCloud = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}
