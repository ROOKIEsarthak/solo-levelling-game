package com.example.solo_levelling.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solo_levelling.R
import com.example.solo_levelling.ui.theme.CascadiaCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val WelcomeBg = Color(0xFF1A0A2E)
private val GlowPurple = Color(0xFF7B2CBF)
private val GlowBlue = Color(0xFF4CC9F0)

/** Pick preferred Japanese TTS voice name: explicit male, else non-female, else first. */
fun preferJapaneseVoiceName(names: List<String>): String? {
    if (names.isEmpty()) return null
    val tagged = names.map { it to it.lowercase(java.util.Locale.ROOT) }
    return tagged.firstOrNull { (_, n) -> n.contains("male") && !n.contains("female") }?.first
        ?: tagged.firstOrNull { (_, n) -> !n.contains("female") }?.first
        ?: names.first()
}

@Composable
fun WelcomeSplash() {
    val context = LocalContext.current
    val ariseAlpha = remember { Animatable(0f) }
    val ariseScale = remember { Animatable(0.55f) }
    val okiroAlpha = remember { Animatable(0f) }

    DisposableEffect(Unit) {
        val appContext = context.applicationContext
        val handler = Handler(Looper.getMainLooper())
        var player: MediaPlayer? = null
        val play = Runnable {
            try {
                val mp = MediaPlayer.create(appContext, R.raw.okiro_deep) ?: return@Runnable
                player = mp
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                mp.setVolume(1f, 1f)
                mp.setOnCompletionListener {
                    it.release()
                    if (player === it) player = null
                }
                mp.start()
            } catch (_: Exception) {
                // Audio unavailable — animation still runs
            }
        }
        handler.postDelayed(play, 450)
        onDispose {
            handler.removeCallbacks(play)
            try {
                player?.stop()
            } catch (_: Exception) {
            }
            try {
                player?.release()
            } catch (_: Exception) {
            }
            player = null
        }
    }

    LaunchedEffect(Unit) {
        launch {
            ariseAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            ariseScale.animateTo(1.12f, tween(550, easing = FastOutSlowInEasing))
            ariseScale.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        }
        delay(400)
        okiroAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    val pulse = rememberInfiniteTransition(label = "glow")
    val glowPulse by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(WelcomeBg),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlowPurple.copy(alpha = 0.55f * glowPulse),
                        GlowBlue.copy(alpha = 0.18f * glowPulse),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.55f,
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(cx, cy),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "ARISE",
                modifier = Modifier.graphicsLayer {
                    alpha = ariseAlpha.value
                    scaleX = ariseScale.value
                    scaleY = ariseScale.value
                },
                color = Color.White,
                fontSize = 52.sp,
                fontFamily = CascadiaCode,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = GlowPurple.copy(alpha = 0.9f),
                        offset = Offset(0f, 0f),
                        blurRadius = 28f,
                    ),
                ),
            )
            Text(
                text = "起きろ",
                modifier = Modifier.graphicsLayer { alpha = okiroAlpha.value },
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = GlowBlue.copy(alpha = 0.7f),
                        offset = Offset(0f, 0f),
                        blurRadius = 18f,
                    ),
                ),
            )
        }
    }
}
