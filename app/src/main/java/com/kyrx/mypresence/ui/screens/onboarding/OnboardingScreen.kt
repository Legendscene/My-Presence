package com.kyrx.mypresence.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyrx.mypresence.ui.theme.DiscordBlurple
import com.kyrx.mypresence.ui.theme.DiscordDark
import com.kyrx.mypresence.ui.theme.DiscordDarker
import com.kyrx.mypresence.ui.theme.DiscordFuchsia
import com.kyrx.mypresence.ui.theme.DiscordGreen
import com.kyrx.mypresence.ui.theme.DiscordText
import com.kyrx.mypresence.ui.theme.DiscordTextMuted
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val gradient: List<*>
)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Filled.SportsEsports,
        title = "Welcome to My Presence",
        description = "Show the world what you're playing. Display your Discord Rich Presence directly from your Android device.",
        gradient = listOf(DiscordBlurple, DiscordFuchsia)
    ),
    OnboardingPage(
        icon = Icons.Filled.TrackChanges,
        title = "Real-Time Presence",
        description = "Your status updates instantly across all Discord clients. Friends will see exactly what you're doing, right now.",
        gradient = listOf(DiscordGreen, DiscordBlurple)
    ),
    OnboardingPage(
        icon = Icons.Filled.Security,
        title = "Secure by Design",
        description = "Your credentials never leave your device. We use Discord's official API with zero server-side token storage.",
        gradient = listOf(DiscordFuchsia, DiscordBlurple)
    ),
    OnboardingPage(
        icon = Icons.Filled.Bolt,
        title = "Lightweight & Fast",
        description = "Built with Kotlin and Jetpack Compose. Minimal battery usage with foreground service precision.",
        gradient = listOf(DiscordBlurple, DiscordGreen)
    )
)

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    var currentPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(pagerState.currentPage) {
        currentPage = pagerState.currentPage
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordDarker)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Brand
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(spring(dampingRatio = Spring.DampingRatioLowBouncy)) +
                        slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(DiscordBlurple, DiscordFuchsia)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SportsEsports,
                            contentDescription = "My Presence Logo",
                            tint = DiscordText,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "My Presence",
                        style = MaterialTheme.typography.headlineLarge,
                        color = DiscordText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                OnboardingPageContent(page = onboardingPages[page])
            }

            // Page indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(onboardingPages.size) { index ->
                    val animatedSize by animateFloatAsState(
                        targetValue = if (currentPage == index) 24f else 8f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "indicator"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(animatedSize.dp)
                            .clip(CircleShape)
                            .background(
                                if (currentPage == index) DiscordBlurple
                                else DiscordTextMuted.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (currentPage < onboardingPages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        } else {
                            onOnboardingComplete()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DiscordBlurple
                    )
                ) {
                    Text(
                        text = if (currentPage < onboardingPages.size - 1) "Next" else "Get Started",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (currentPage < onboardingPages.size - 1) {
                    OutlinedButton(
                        onClick = { onOnboardingComplete() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.titleMedium,
                            color = DiscordTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = page.gradient.filterIsInstance<androidx.compose.ui.graphics.Color>()
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = DiscordText,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.displayMedium,
            color = DiscordText,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = DiscordTextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(0.8f)
        )
    }
}
