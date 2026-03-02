package com.dipdev.btkeyboard.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dipdev.btkeyboard.ui.theme.CyanPrimary
import com.dipdev.btkeyboard.ui.theme.NavyBorder
import com.dipdev.btkeyboard.ui.theme.NavyDeep
import com.dipdev.btkeyboard.ui.theme.SuccessGreen
import com.dipdev.btkeyboard.ui.theme.TextPrimary
import com.dipdev.btkeyboard.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// ── Data model for each page ───────────────────────────────────────────────

private data class OnboardPage(
    val icon: String,
    val title: String,
    val body: String,
    val accentColor: Color,
    val gradientBottom: Color
)

private val pages = listOf(
    OnboardPage(
        icon = "⌨️",
        title = "Your Phone,\nYour Keyboard",
        body = "Turn any Android phone into a full Bluetooth keyboard for your PC, Mac, or smart TV — no cables, no dongles.",
        accentColor = CyanPrimary,
        gradientBottom = Color(0xFF091828)
    ),
    OnboardPage(
        icon = "🔗",
        title = "Three Simple\nSteps",
        body = "① Enable Bluetooth on both devices\n② Tap Start — your phone begins advertising\n③ Pair from your computer and start typing!",
        accentColor = SuccessGreen,
        gradientBottom = Color(0xFF081B10)
    ),
    OnboardPage(
        icon = "📡",
        title = "Bluetooth Access\nNeeded",
        body = "BT Keyboard needs Bluetooth permission to advertise as a keyboard and connect to your devices. No data is collected or sent anywhere.",
        accentColor = Color(0xFFB388FF),
        gradientBottom = Color(0xFF100A1E)
    )
)

// ── Onboarding Screen ─────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onSkip: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Box(modifier = Modifier
        .fillMaxSize()
        .background(NavyDeep)) {

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val page = pages[pageIndex]
            val pageOffset = (pagerState.currentPage - pageIndex +
                    pagerState.currentPageOffsetFraction).absoluteValue

            OnboardPageContent(
                page = page,
                pageOffset = pageOffset
            )
        }

        // Skip button — top right
        if (!isLastPage) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
            ) {
                Text(
                    "Skip",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { i ->
                    val isSelected = pagerState.currentPage == i
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 28.dp else 8.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "indicator_width"
                    )
                    val color by animateColorAsState(
                        targetValue = if (isSelected) pages[i].accentColor else NavyBorder,
                        label = "indicator_color"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            // CTA button
            val currentPage = pages[pagerState.currentPage]
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinished()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentPage.accentColor,
                    contentColor = NavyDeep
                )
            ) {
                Text(
                    if (isLastPage) "Grant & Continue" else "Next",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Single page content ────────────────────────────────────────────────────

@Composable
private fun OnboardPageContent(page: OnboardPage, pageOffset: Float) {
    val scale = 1f - (pageOffset * 0.12f)
    val alpha = 1f - (pageOffset * 0.6f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDeep, page.gradientBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Icon circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(page.accentColor.copy(alpha = 0.15f))
                    .border(2.dp, page.accentColor.copy(alpha = 0.4f), CircleShape)
            ) {
                Text(
                    page.icon,
                    fontSize = 56.sp
                )
            }

            // Title
            Text(
                page.title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                lineHeight = 38.sp
            )

            // Accent divider
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(page.accentColor)
            )

            // Body
            Text(
                page.body,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
        }
    }
}
