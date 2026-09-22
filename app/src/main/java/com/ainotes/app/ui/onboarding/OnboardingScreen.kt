package com.ainotes.app.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ainotes.app.AppContainer
import com.ainotes.app.R
import com.ainotes.app.ui.i18n.txt
import com.ainotes.app.ui.settings.ProviderFormDialog
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    container: AppContainer,
    onDone: () -> Unit
) {
    // 0 = language, 1 = welcome, 2 = AI setup, 3 = provider form
    var step by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        when (step) {
            0 -> {
                Text(
                    "زبان / Language",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "فارسی: راست‌نویس\nEnglish: left-to-right",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))

                LanguageOption(
                    title = "فارسی",
                    subtitle = "راست‌نویس (RTL)",
                    onClick = {
                        scope.launch { container.settings.setLanguage("fa") }
                        step = 1
                    }
                )
                Spacer(Modifier.height(12.dp))
                LanguageOption(
                    title = "English",
                    subtitle = "left-to-right (LTR)",
                    onClick = {
                        scope.launch { container.settings.setLanguage("en") }
                        step = 1
                    }
                )
            }

            1 -> {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    txt("به یادداشت هوشمند خوش اومدی", "Welcome to AI Notes"),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    txt(
                        "یادداشت‌هات.\nفایل‌هات.\nهوش مصنوعی‌ات.",
                        "Your notes.\nYour files.\nYour AI."
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(36.dp))
                Button(
                    onClick = { step = 2 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) { Text(txt("شروع", "Get Started")) }
            }

            else -> {
                Text(
                    txt("تنظیم AI", "Configure AI"),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    txt(
                        "همین الان سرویس AI خودت رو اضافه کن — آدرس، کلید و مدل دلخواهت — یا بعداً. یادداشت‌ها بدون AI هم کاملاً آفلاین کار میکنن.",
                        "Add an AI provider now - your own URL, key and model - or do it later. Notes work fully offline without AI."
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(36.dp))
                Button(
                    onClick = { step = 3 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) { Text(txt("افزودن سرویس AI", "Add AI Provider")) }
                TextButton(
                    onClick = {
                        scope.launch { container.settings.setOnboardingDone() }
                        onDone()
                    }
                ) { Text(txt("بعداً", "Skip")) }
            }
        }

        Spacer(Modifier.weight(1f))
    }

    if (step == 3) {
        ProviderFormDialog(
            container = container,
            onSaved = {
                scope.launch { container.settings.setOnboardingDone() }
                onDone()
            },
            onDismiss = {
                scope.launch { container.settings.setOnboardingDone() }
                onDone()
            }
        )
    }
}

@Composable
private fun LanguageOption(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}
