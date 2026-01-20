package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.ui.components.PrimaryActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroScreen(
    onContinue: () -> Unit
) {
    Scaffold { padding ->
        var bottomBarHeightPx by remember { mutableStateOf(0) }
        val bottomBarHeight = with(LocalDensity.current) {
            val measured = bottomBarHeightPx.toDp()
            if (measured.value > 0f) measured else 88.dp
        }
        val bottomBarBrush = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                MaterialTheme.colorScheme.surface
            )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomBarHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.Start),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_intro_logo),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(
                                if (isSystemInDarkTheme()) {
                                    Color.White
                                } else {
                                    Color.Black
                                }
                            ),
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                        )
                    }
                    Text(
                        text = stringResource(R.string.intro_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = stringResource(R.string.intro_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val featureItems = listOf(
                            Triple(
                                "⚡",
                                stringResource(R.string.intro_feature_auto),
                                MaterialTheme.colorScheme.primary
                            ),
                            Triple(
                                "🧠",
                                stringResource(R.string.intro_feature_merge),
                                MaterialTheme.colorScheme.secondary
                            ),
                            Triple(
                                "🙌",
                                stringResource(R.string.intro_feature_override),
                                MaterialTheme.colorScheme.tertiary
                            ),
                            Triple(
                                "🔒",
                                stringResource(R.string.intro_feature_privacy),
                                MaterialTheme.colorScheme.primary
                            ),
                            Triple(
                                "🧪",
                                stringResource(R.string.intro_feature_keywords),
                                MaterialTheme.colorScheme.secondary
                            )
                        )

                        featureItems.forEach { (emoji, text, color) ->
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = color)) {
                                        append("$emoji ")
                                    }
                                    append(text)
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.intro_notice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.intro_vibe_coded),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(bottomBarBrush)
                    .onSizeChanged { bottomBarHeightPx = it.height }
                    .padding(top = 18.dp, bottom = 14.dp)
            ) {
                PrimaryActionButton(
                    label = stringResource(R.string.continue_button),
                    onClick = onContinue
                )
            }
        }
    }
}
