package com.kasumi.boots.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasumi.boots.core.BoostExecutor
import com.kasumi.boots.ui.theme.KasumiBootsTheme
import com.kasumi.boots.ui.theme.NeonGreen
import com.kasumi.boots.ui.theme.TextSecondary
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure Shell is cached
        Shell.getShell {}

        setContent {
            KasumiBootsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var statusText by remember { mutableStateOf("Ready") }
    var descriptionText by remember { mutableStateOf("Nhấn vào nút bên dưới để tối ưu hóa hệ thống.") }
    var isBoosting by remember { mutableStateOf(false) }
    var boostProgress by remember { mutableStateOf(0f) }
    var boostLogs by remember { mutableStateOf(listOf<String>()) }
    var resultSuccess by remember { mutableStateOf<Boolean?>(null) }

    val scrollState = rememberScrollState()

    fun performBoost() {
        scope.launch {
            isBoosting = true
            boostProgress = 0f
            boostLogs = emptyList()
            resultSuccess = null
            statusText = "Checking Root..."
            
            val hasRoot = withContext(Dispatchers.IO) {
                try {
                    withTimeout(5000) {
                        suspendCoroutine<Boolean> { cont ->
                            Shell.getShell { shell ->
                                cont.resume(shell.isRoot)
                            }
                        }
                    }
                } catch (e: Exception) {
                    false
                }
            }

            if (!hasRoot) {
                statusText = "Root Denied"
                descriptionText = "Ứng dụng cần quyền Root để hoạt động."
                resultSuccess = false
                isBoosting = false
                return@launch
            }

            statusText = "Boosting..."
            descriptionText = "Đang thực hiện tối ưu hóa..."

            val executor = BoostExecutor()
            val result = executor.execute { log ->
                boostLogs = boostLogs + log
                // Auto scroll to bottom of logs?
            }

            if (result.success) {
                statusText = "Hoàn tất"
                descriptionText = "Hệ thống đã được tối ưu hóa thành công."
                resultSuccess = true
            } else {
                statusText = "Thất bại"
                descriptionText = result.errors.joinToString("\n")
                resultSuccess = false
            }

            isBoosting = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        // Header
        Text(
            text = "KASUMI BOOTS",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Text(
            text = "Tối Ưu Hiệu Năng",
            style = MaterialTheme.typography.labelLarge.copy(
                color = TextSecondary,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                // Icon or Loading
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBoosting) {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            color = NeonGreen,
                            strokeWidth = 4.dp
                        )
                    } else {
                        val iconVector = when (resultSuccess) {
                            true -> Icons.Default.Check
                            false -> Icons.Default.Close
                            else -> Icons.Default.Bolt
                        }

                        val iconColor = when (resultSuccess) {
                            true -> Color.Green
                            false -> Color.Red
                            else -> NeonGreen
                        }

                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = iconColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = if (isBoosting) NeonGreen else MaterialTheme.colorScheme.onSurface
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logs Area (Visible when logs exist)
        AnimatedVisibility(visible = boostLogs.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 0.dp, max = 200.dp)
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF12121A)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    boostLogs.forEach { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFFAAAAAA)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Boost Button
        Button(
            onClick = { performBoost() },
            enabled = !isBoosting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = if (isBoosting) 0.dp else 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = NeonGreen
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGreen,
                disabledContainerColor = Color(0xFF1E2F2A)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isBoosting) {
                Text(
                    text = "Đang xử lý...",
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TĂNG TỐC NGAY",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
