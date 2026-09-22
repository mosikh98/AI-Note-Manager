package com.ainotes.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ainotes.app.R

@Composable
fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_empty_notes),
            contentDescription = null,
            modifier = Modifier.size(88.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LoadingDots(label: String) {
    val transition = rememberInfiniteTransition(label = "dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "phase"
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(0.7f + ((phase + i * 0.33f) % 1f) * 0.6f)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                if (i < 2) Spacer(Modifier.width(6.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)
    )
}
