package com.kyrx.mypresence.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import com.kyrx.mypresence.ui.components.PremiumButton
import com.kyrx.mypresence.ui.components.PremiumOutlinedButton
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Blurple
import com.kyrx.mypresence.ui.theme.Cyan
import com.kyrx.mypresence.ui.theme.Gold
import com.kyrx.mypresence.ui.theme.GoldLight
import com.kyrx.mypresence.ui.theme.GradientGold
import com.kyrx.mypresence.ui.theme.SurfaceMid
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import com.kyrx.mypresence.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Filled.VideogameAsset,
        title = "Welcome to My Presence",
        description = "Show your Discord Rich Presence right from your Android device. Stay connected with style."
    ),
    OnboardingPage(
        icon = Icons.Filled.Speed,
        title = "Real-Time Presence",
        description = "Your activity, status, and mood — updated in real time on Discord. Fast and reliable."
    ),
    OnboardingPage(
        icon = Icons.Filled.Security,
        title = "Secure by Design",
        description = "OAuth2 with PKCE. Encrypted storage. Certificate pinning. Your data stays yours."
    ),
    OnboardingPage(
        icon = Icons.Filled.Bolt,
        title = "Lightweight & Fast",
        description = "Optimized for performance. Smooth animations. Minimal battery usage."
    )
)

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    preferencesRepository: PreferencesRepository?
) {
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == pages.size - 1) {
            delay(5000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(32.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        if (pagerState.currentPage < pages.size - 1) {
            Text(
                text = "Skip",
                color = TextTertiary,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceMid.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(36.dp))
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) { pageIndex ->
            val page = pages[pageIndex]
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                        slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(GoldLight, Gold)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = Background,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Text(
                        text = page.title,
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.W700,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = page.description,
                        color = TextSecondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )
                }
            }
        }

        // Page Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (isSelected) 28.dp else 8.dp, height = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .then(
                            if (isSelected) Modifier.background(
                                Brush.horizontalGradient(GradientGold),
                                RoundedCornerShape(4.dp)
                            ) else Modifier.background(
                                TextTertiary.copy(alpha = 0.3f),
                                RoundedCornerShape(4.dp)
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        if (pagerState.currentPage < pages.size - 1) {
            PremiumButton(
                text = "Next",
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                gradient = GradientGold
            )
        } else {
            PremiumButton(
                text = "Get Started",
                onClick = {
                    scope.launch {
                        preferencesRepository?.setOnboardingCompleted(true)
                        onOnboardingComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                gradient = GradientGold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
