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
import androidx.compose.material3.RadioButton
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
import com.ainotes.app.sync.CloudConfig
import com.ainotes.app.sync.CloudKinds
import com.ainotes.app.sync.CloudManager
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

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
    var showCrashLog by remember { mutableStateOf(false) }
    var cloudKind by remember {
        mutableStateOf(container.secureStore.get("cloud_kind") ?: CloudKinds.CUSTOM)
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
                txt("سرویس ابری", "Cloud service"),
                kindLabel(cloudKind)
            ) { showCloud = true }
            SettingRow(
                txt("پشتیبان‌گیری اکنون", "Backup now"),
                if (backingUp) txt("در حال آپلود...", "Uploading...")
                else txt(
                    "همهٔ یادداشت‌ها رو به سرویس ابری میفرسته — فقط با دستور خودت",
                    "Uploads all notes to your cloud service - only on your command"
                )
            ) { showBackup = true }

            SettingRow(
                txt("زبان", "Language"),
                if (lang == "en") "English — left-to-right" else "فارسی — راست‌نویس"
            ) { showLang = true }

            SectionLabel(txt("درباره", "About"))
            SettingRow(txt("نسخهٔ اپ", "App version"), "1.0.0") { }
            SettingRow(txt("کتابخانه‌های متن‌باز", "Open-source licenses"), "Compose, Room, OkHttp, WorkManager") { }
            SettingRow(
                txt("لاگ کرش", "Crash log"),
                txt("نمایش، کپی و پاک‌کردن آخرین خطای کرش", "View, copy and clear the last crash")
            ) { showCrashLog = true }

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
        val backupDest = kindLabel(cloudKind)
        val msgNoService = txt(
            "اول سرویس ابری رو تنظیم کن (بخش ذخیرهٔ ابری).",
            "Set up a cloud service first (Cloud storage section)."
        )
        val msgIncomplete = txt(
            "اطلاعات اتصال ناقصه — اول سرویس رو کامل پیکربندی کن.",
            "Connection details are incomplete - finish the service setup first."
        )
        val msgOkTpl = txt(
            "{n} یادداشت با موفقیت آپلود شد.",
            "{n} notes uploaded successfully."
        )
        AlertDialog(
            onDismissRequest = { showBackup = false },
            title = { Text(txt("آپلود به سرویس ابری؟", "Upload to cloud service?")) },
            text = {
                Text(
                    txt("یادداشت‌هات به این مقصد فرستاده میشه: ", "Your notes will be sent to: ") +
                        backupDest + ".\n\n" +
                        txt(
                            "آپلود فقط وقتی انجام میشه که خودت «شروع پشتیبان‌گیری» رو بزنی — هیچ‌چیز بی‌صدا فرستاده نمیشه.",
                            "Uploading happens only when you tap Start backup - nothing is sent silently."
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
                        val cloud = CloudManager(container.secureStore)
                        val cfg = cloud.load()
                        scope.launch(Dispatchers.IO) {
                            cloudStatus = if (!cloud.isConfigured(cfg)) {
                                if (cfg.url.isBlank()) msgNoService else msgIncomplete
                            } else {
                                val notes = runCatching { container.repository.allNotes() }
                                    .getOrDefault(emptyList())
                                cloud.backup(notes).fold(
                                    onSuccess = { msgOkTpl.replace("{n}", notes.size.toString()) },
                                    onFailure = { e ->
                                        "\u274C " + (e.message ?: e.javaClass.simpleName)
                                    }
                                )
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
        val cloud = remember { CloudManager(container.secureStore) }
        val stored = remember { cloud.load() }
        var cKind by remember { mutableStateOf(stored.kind) }
        var cUrl by remember { mutableStateOf(stored.url) }
        var cUser by remember { mutableStateOf(stored.username) }
        var cPass by remember { mutableStateOf(maskOrEmpty(stored.password)) }
        var cToken by remember { mutableStateOf(maskOrEmpty(stored.token)) }
        var cKey by remember { mutableStateOf(maskOrEmpty(stored.apiKey)) }
        var testing by remember { mutableStateOf(false) }
        var testMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCloud = false },
            title = { Text(txt("ذخیرهٔ ابری", "Cloud storage")) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    CloudKinds.ALL.forEach { k ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    cKind = k
                                    testMsg = null
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = cKind == k,
                                onClick = {
                                    cKind = k
                                    testMsg = null
                                }
                            )
                            Text(kindLabel(k), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    when (cKind) {
                        CloudKinds.CUSTOM -> {
                            CloudField(
                                txt("آدرس سرور (endpoint)", "Server URL (endpoint)"),
                                cUrl
                            ) { cUrl = it }
                            CloudField(
                                txt("کلید API (اختیاری)", "API key (optional)"),
                                cKey
                            ) { cKey = it }
                        }
                        CloudKinds.WEBDAV -> {
                            CloudField(
                                txt("آدرس پوشهٔ WebDAV", "WebDAV folder URL"),
                                cUrl,
                                hint = txt(
                                    "مثلاً Nextcloud: .../remote.php/dav/files/USER/",
                                    "e.g. Nextcloud: .../remote.php/dav/files/USER/"
                                )
                            ) { cUrl = it }
                            CloudField(txt("نام کاربری", "Username"), cUser) { cUser = it }
                            CloudField(
                                txt("رمز عبور", "Password"),
                                cPass,
                                password = true
                            ) { cPass = it }
                        }
                        else -> {
                            CloudField(
                                txt("توکن دسترسی", "Access token"),
                                cToken,
                                password = true
                            ) { cToken = it }
                            Text(
                                tokenHint(cKind),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            enabled = !testing && fieldsPresent(cKind, cUrl, cUser, cToken),
                            onClick = {
                                testing = true
                                testMsg = null
                                val cfg = CloudConfig(
                                    kind = cKind,
                                    url = cUrl.trim(),
                                    apiKey = unmask(cKey, stored.apiKey),
                                    username = cUser.trim(),
                                    password = unmask(cPass, stored.password),
                                    token = unmask(cToken, stored.token)
                                )
                                scope.launch(Dispatchers.IO) {
                                    testMsg = cloud.test(cfg).fold(
                                        onSuccess = { "\u2705 " + it },
                                        onFailure = { e ->
                                            "\u274C " + (e.message ?: e.javaClass.simpleName)
                                        }
                                    )
                                    testing = false
                                }
                            }
                        ) { Text(txt("تست اتصال", "Test connection")) }
                        if (testing) {
                            Spacer(Modifier.padding(8.dp))
                            CircularProgressIndicator(
                                Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    testMsg?.let { m ->
                        Text(
                            m,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (m.startsWith("\u2705")) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Text(
                        txt(
                            "رمز/توکن فقط رمزنگاری‌شده روی همین دستگاه ذخیره میشه و هیچ‌وقت وارد کد نمیشه.",
                            "Passwords/tokens are stored encrypted on this device and never written into code."
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        container.secureStore.put("cloud_kind", cKind)
                        container.secureStore.put("cloud_url", cUrl.trim())
                        if (!isMasked(cKey)) container.secureStore.put("cloud_key", cKey.trim())
                        container.secureStore.put("cloud_user", cUser.trim())
                        if (!isMasked(cPass)) container.secureStore.put("cloud_pass", cPass.trim())
                        if (!isMasked(cToken)) container.secureStore.put("cloud_token", cToken.trim())
                        cloudKind = cKind
                        showCloud = false
                    }
                ) { Text(txt("ذخیره", "Save")) }
            },
            dismissButton = {
                TextButton(onClick = { showCloud = false }) { Text(txt("انصراف", "Cancel")) }
            }
        )
    }

    if (showCrashLog) {
        val trace = remember {
            runCatching {
                java.io.File(container.app.filesDir, "crash.log")
                    .takeIf { it.exists() }?.readText()
            }.getOrNull()
        }
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showCrashLog = false },
            title = { Text(txt("لاگ کرش", "Crash log")) },
            text = {
                Text(
                    trace
                        ?: txt(
                            "لاگی ثبت نشده — هنوز کرشی رخ نداده یا پاک شده.",
                            "No crash recorded yet."
                        ),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(
                    enabled = trace != null,
                    onClick = { trace?.let { t -> clipboard.setText(AnnotatedString(t)) } }
                ) { Text(txt("کپی", "Copy")) }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            runCatching {
                                java.io.File(container.app.filesDir, "crash.log").delete()
                            }
                            showCrashLog = false
                        }
                    ) { Text(txt("پاک کردن", "Clear")) }
                    TextButton(onClick = { showCrashLog = false }) {
                        Text(txt("بستن", "Close"))
                    }
                }
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

private const val CLOUD_MASK = "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022"

private fun isMasked(v: String) = v.contains('\u2022')
private fun maskOrEmpty(v: String) = if (v.isBlank()) "" else CLOUD_MASK
private fun unmask(value: String, stored: String) = if (isMasked(value)) stored else value

private fun fieldsPresent(kind: String, url: String, user: String, token: String) =
    when (kind) {
        CloudKinds.CUSTOM -> url.isNotBlank()
        CloudKinds.WEBDAV -> url.isNotBlank() && user.isNotBlank()
        else -> token.isNotBlank()
    }

@Composable
private fun kindLabel(kind: String): String = when (kind) {
    CloudKinds.WEBDAV -> "WebDAV (Nextcloud, Box, pCloud)"
    CloudKinds.DROPBOX -> "Dropbox"
    CloudKinds.GDRIVE -> "Google Drive"
    CloudKinds.ONEDRIVE -> "OneDrive (Microsoft)"
    else -> txt("سرور اختصاصی (HTTP)", "Custom server (HTTP)")
}

@Composable
private fun tokenHint(kind: String): String = when (kind) {
    CloudKinds.DROPBOX -> txt(
        "Dropbox App Console \u2192 دکمه Generated access token.",
        "Dropbox App Console \u2192 Generated access token button."
    )
    CloudKinds.GDRIVE -> txt(
        "Google OAuth Playground \u2192 scope drive \u2192 توکن.",
        "Google OAuth Playground \u2192 drive scope \u2192 token."
    )
    else -> txt(
        "Microsoft Graph Explorer \u2192 توکن با scope Files.ReadWrite.",
        "Microsoft Graph Explorer \u2192 token with Files.ReadWrite scope."
    )
}

@Composable
private fun CloudField(
    label: String,
    value: String,
    password: Boolean = false,
    hint: String? = null,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation()
        else VisualTransformation.None
    )
    if (hint != null) {
        Text(
            hint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
    }
}