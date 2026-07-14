package com.kyrx.mypresence.feature.settings

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyrx.mypresence.BuildConfig
import com.kyrx.mypresence.ui.animation.StaggeredReveal
import com.kyrx.mypresence.ui.components.AnimatedLogo
import com.kyrx.mypresence.ui.components.GlassCard
import com.kyrx.mypresence.ui.components.GradientText
import com.kyrx.mypresence.ui.theme.Accent
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Surface
import com.kyrx.mypresence.ui.theme.SurfaceBorder
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import com.kyrx.mypresence.ui.theme.TextTertiary
import com.kyrx.mypresence.ui.theme.Warning

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var devTapCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            GradientText(
                text = "About",
                fontSize = 24.sp,
                fontWeight = FontWeight.W700
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            StaggeredReveal(0) {
                AnimatedLogo(size = 72.dp, showOrbits = true)
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredReveal(1) {
                GradientText(
                    text = "My Presence",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.W700
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            StaggeredReveal(2) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            StaggeredReveal(3) {
                GlassCard {
                    AboutInfoTile(
                        icon = Icons.Filled.Info,
                        iconColor = Accent,
                        title = "Version",
                        subtitle = "${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AboutLinkTile(
                        icon = Icons.Filled.Code,
                        iconColor = Accent,
                        title = "Git Commit",
                        subtitle = BuildConfig.GIT_COMMIT,
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("git commit", BuildConfig.GIT_COMMIT))
                            Toast.makeText(context, "Commit copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AboutLinkTile(
                        icon = Icons.Filled.Description,
                        iconColor = Accent,
                        title = "Open Source Licenses",
                        subtitle = "View third-party licenses",
                        onClick = {
                            try {
                                context.startActivity(Intent(context, Class.forName("com.google.android.gms.oss.licenses.OssLicensesActivity")))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Licenses not available", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            StaggeredReveal(4) {
                GlassCard {
                    AboutLinkTile(
                        icon = Icons.Filled.Shield,
                        iconColor = Warning,
                        title = "Privacy Policy",
                        subtitle = "How we handle your data",
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://mypresence.app/privacy")))
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AboutLinkTile(
                        icon = Icons.Filled.Description,
                        iconColor = Accent,
                        title = "Terms of Service",
                        subtitle = "App usage terms",
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://mypresence.app/terms")))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            StaggeredReveal(5) {
                GlassCard {
                    AboutLinkTile(
                        icon = Icons.Filled.Person,
                        iconColor = Accent,
                        title = "Developer",
                        subtitle = "Built by KyRx",
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/anomalyco")))
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AboutLinkTile(
                        icon = Icons.Filled.Build,
                        iconColor = Accent,
                        title = "Support & Feedback",
                        subtitle = "Report issues or suggest features",
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/anomalyco/mypresence/issues")))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            StaggeredReveal(6) {
                Text(
                    text = "Made with ❤ using Kotlin & Jetpack Compose",
                    color = TextTertiary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            devTapCount++
                            if (devTapCount >= 7) {
                                devTapCount = 0
                                Toast.makeText(context, "Developer mode enabled!", Toast.LENGTH_SHORT).show()
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AboutInfoTile(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.W600
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AboutLinkTile(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.W600
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
