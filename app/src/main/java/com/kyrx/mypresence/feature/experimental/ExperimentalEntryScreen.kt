package com.kyrx.mypresence.feature.experimental

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyrx.mypresence.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalEntryScreen(onBack: () -> Unit = {}) {
    var unlocked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Experimental") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Blurple.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", fontSize = 36.sp)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Experimental Features",
                fontSize = 22.sp,
                fontWeight = FontWeight.W700,
                color = TextPrimary
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "These features are in active development and may be unstable. " +
                        "Enable at your own risk.",
                fontSize = 14.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { unlocked = !unlocked },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (unlocked) Success else Blurple)
            ) {
                Text(
                    if (unlocked) "Disable Experimental Mode" else "Enable Experimental Mode",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))

            if (unlocked) {
                Text(
                    text = "Experimental mode enabled. New features will appear here.",
                    color = Success,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Coming Soon",
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        listOf(
                            "Advanced Rich Presence customization",
                            "Custom activity images & timers",
                            "Bluetooth device presence detection",
                            "Wear OS companion support"
                        ).forEach { feature ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Gold)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    feature,
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
