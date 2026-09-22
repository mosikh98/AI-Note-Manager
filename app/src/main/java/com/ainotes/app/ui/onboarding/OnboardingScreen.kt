package com.ainotes.app.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.ainotes.app.ui.settings.ProviderFormDialog
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    container: AppContainer,
    onDone: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null
        )
        Spacer(Modifier.height(28.dp))

        if (step == 0) {
            Text(
                "به یادداشت هوشمند خوش اومدی",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "یادداشت‌هات.\nفایل‌هات.\nهوش مصنوعی‌ات.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(36.dp))
            Button(
                onClick = { step = 1 },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) { Text("شروع") }
        } else {
            Text(
                "تنظیم AI",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "همین الان سرویس AI خودت رو اضافه کن — آدرس، کلید و مدل دلخواهت — یا بعداً. یادداشت‌ها بدون AI هم کاملاً آفلاین کار میکنن.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(36.dp))
            Button(
                onClick = { step = 2 },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) { Text("افزودن سرویس AI") }
            TextButton(
                onClick = {
                    scope.launch { container.settings.setOnboardingDone() }
                    onDone()
                }
            ) { Text("بعداً") }
        }

        Spacer(Modifier.weight(1f))
    }

    if (step == 2) {
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
