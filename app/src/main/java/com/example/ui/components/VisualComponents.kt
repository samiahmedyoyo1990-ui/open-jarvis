package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.theme.BackgroundStylePreset
import com.example.data.theme.OrbAnimationPreset
import com.example.data.theme.ThemeConfig
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RedAlert
import com.example.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    EXECUTING,
    SPEAKING,
    ERROR
}

/**
 * Premium Siri-inspired fluid animated background visualizer that sits behind the interface.
 * Reacts to active ThemeConfig (fluid mesh, gradient, or solid), state, and audio amplitude.
 */
@Composable
fun SiriFluidVisualizerBackground(
    state: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    theme: ThemeConfig = JarvisTheme.config,
    reducedMotion: Boolean = false
) {
    if (reducedMotion || theme.backgroundStyle == BackgroundStylePreset.SOLID) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
        )
        return
    }

    if (theme.backgroundStyle == BackgroundStylePreset.GRADIENT) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            theme.backgroundColor,
                            theme.surfaceColor,
                            theme.backgroundColor
                        )
                    )
                )
        )
        return
    }

    val speedMult = theme.animationSpeedMultiplier.coerceIn(0.4f, 2.5f)
    val transition = rememberInfiniteTransition(label = "fluidMotion")

    val t1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (11000 / speedMult).toInt().coerceAtLeast(1800), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t1"
    )

    val t2 by transition.animateFloat(
        initialValue = 6.28318f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (15000 / speedMult).toInt().coerceAtLeast(2500), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t2"
    )

    val breathingScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.LISTENING -> (1300 / speedMult).toInt()
                    AssistantState.THINKING -> (800 / speedMult).toInt()
                    AssistantState.SPEAKING -> (1100 / speedMult).toInt()
                    else -> (3000 / speedMult).toInt()
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val primaryAccent = theme.primaryAccent
    val secondaryAccent = theme.secondaryAccent
    val tertiaryAccent = theme.tertiaryAccent

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height * 0.36f

        val effectiveAmp = amplitude.coerceIn(0.05f, 1.0f)
        val stateScale = when (state) {
            AssistantState.IDLE -> 0.85f * breathingScale
            AssistantState.LISTENING -> (1.15f + effectiveAmp * 0.5f) * breathingScale
            AssistantState.THINKING -> 1.08f * breathingScale
            AssistantState.EXECUTING -> 1.02f * breathingScale
            AssistantState.SPEAKING -> (1.12f + effectiveAmp * 0.45f) * breathingScale
            AssistantState.ERROR -> 0.95f
        }

        val (coreColor, secondColor, thirdColor) = when (state) {
            AssistantState.IDLE -> Triple(primaryAccent, secondaryAccent, tertiaryAccent)
            AssistantState.LISTENING -> Triple(primaryAccent, tertiaryAccent, secondaryAccent)
            AssistantState.THINKING -> Triple(secondaryAccent, primaryAccent, tertiaryAccent)
            AssistantState.EXECUTING -> Triple(tertiaryAccent, primaryAccent, secondaryAccent)
            AssistantState.SPEAKING -> Triple(primaryAccent, secondaryAccent, tertiaryAccent)
            AssistantState.ERROR -> Triple(RedAlert, Color(0xFFFF9100), Color(0xFF6B0000))
        }

        // 1. Ambient Background Aura
        val auraRadius = width * 0.76f * stateScale * theme.orbGlowIntensity
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    coreColor.copy(alpha = if (state == AssistantState.IDLE) 0.13f else 0.24f),
                    secondColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = auraRadius
            ),
            radius = auraRadius,
            center = Offset(centerX, centerY)
        )

        // 2. Three fluid organic floating energy blobs (Siri-like harmonic lighting)
        val orbRadius = (width * 0.32f) * stateScale

        // Blob 1: Primary Accent
        val blob1X = centerX + (cos(t1.toDouble()) * (width * 0.13f)).toFloat()
        val blob1Y = centerY + (sin(t1.toDouble()) * (height * 0.06f)).toFloat()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    coreColor.copy(alpha = if (state == AssistantState.LISTENING) 0.38f else 0.25f),
                    coreColor.copy(alpha = 0.09f),
                    Color.Transparent
                ),
                center = Offset(blob1X, blob1Y),
                radius = orbRadius * 1.25f
            ),
            radius = orbRadius * 1.25f,
            center = Offset(blob1X, blob1Y)
        )

        // Blob 2: Secondary Accent
        val blob2X = centerX + (cos(t2.toDouble() + 2.1) * (width * 0.14f)).toFloat()
        val blob2Y = centerY + (sin(t2.toDouble() + 2.1) * (height * 0.07f)).toFloat()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    thirdColor.copy(alpha = 0.28f),
                    secondColor.copy(alpha = 0.09f),
                    Color.Transparent
                ),
                center = Offset(blob2X, blob2Y),
                radius = orbRadius * 1.18f
            ),
            radius = orbRadius * 1.18f,
            center = Offset(blob2X, blob2Y)
        )

        // Blob 3: Tertiary Light Center
        val blob3X = centerX + (sin((t1 * 1.4f).toDouble()) * (width * 0.08f)).toFloat()
        val blob3Y = centerY + (cos((t1 * 1.4f).toDouble()) * (height * 0.05f)).toFloat()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondColor.copy(alpha = 0.32f),
                    coreColor.copy(alpha = 0.11f),
                    Color.Transparent
                ),
                center = Offset(blob3X, blob3Y),
                radius = orbRadius * 1.08f
            ),
            radius = orbRadius * 1.08f,
            center = Offset(blob3X, blob3Y)
        )

        // 3. Floating Ambient Particles (if enabled)
        if (theme.ambientParticlesEnabled) {
            val count = (14 * theme.particleIntensity).toInt().coerceIn(6, 30)
            for (i in 0 until count) {
                val angle = (t1 * 0.85f + i * (6.28318f / count))
                val dist = (width * 0.36f) * ((i % 4 + 1) / 4f) * stateScale
                val px = centerX + (cos(angle.toDouble()) * dist).toFloat()
                val py = centerY + (sin(angle.toDouble()) * (dist * 0.72f)).toFloat()
                val pSize = if (i % 2 == 0) 3.5.dp.toPx() else 2.dp.toPx()
                val pAlpha = if (state == AssistantState.THINKING) 0.65f else 0.35f

                drawCircle(
                    color = if (i % 2 == 0) primaryAccent.copy(alpha = pAlpha) else thirdColor.copy(alpha = pAlpha),
                    radius = pSize,
                    center = Offset(px, py)
                )
            }
        }

        // 4. Harmonic soundwave ring when interacting
        if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
            drawHarmonicWaveRing(
                center = Offset(centerX, centerY),
                baseRadius = width * 0.28f * stateScale,
                amplitude = effectiveAmp * 25.dp.toPx(),
                frequency = 8,
                phase = t1 * 3.5f,
                color = coreColor.copy(alpha = 0.45f),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawHarmonicWaveRing(
    center: Offset,
    baseRadius: Float,
    amplitude: Float,
    frequency: Int,
    phase: Float,
    color: Color,
    strokeWidth: Float
) {
    val path = Path()
    val steps = 120
    for (i in 0..steps) {
        val angle = (i.toFloat() / steps) * 6.283185307f
        val r = baseRadius + (sin(angle * frequency + phase) * amplitude)
        val x = center.x + (cos(angle) * r)
        val y = center.y + (sin(angle) * r)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

/**
 * Universal Customizable AI Orb.
 * Supports all 7 distinct presets:
 * SIRI, PLASMA, GALAXY, CYBERNETIC, MINIMAL, LIQUID, HOLOGRAPHIC
 */
@Composable
fun JarvisArcOrb(
    state: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    size: Dp = JarvisTheme.config.orbSizeDp.dp,
    theme: ThemeConfig = JarvisTheme.config
) {
    val speedMult = theme.animationSpeedMultiplier.coerceIn(0.4f, 2.5f)
    val infiniteTransition = rememberInfiniteTransition(label = "OrbMasterTransition")

    val continuousRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (13000 / speedMult).toInt().coerceAtLeast(1800), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuousRot"
    )

    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING -> (2200 / speedMult).toInt()
                    AssistantState.LISTENING -> (5000 / speedMult).toInt()
                    else -> (8500 / speedMult).toInt()
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverseRot"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1900 / speedMult).toInt().coerceAtLeast(500),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    val primaryColor = when (state) {
        AssistantState.IDLE -> theme.primaryAccent
        AssistantState.LISTENING -> theme.primaryAccent
        AssistantState.THINKING -> theme.secondaryAccent
        AssistantState.EXECUTING -> theme.tertiaryAccent
        AssistantState.SPEAKING -> NeonEmerald
        AssistantState.ERROR -> RedAlert
    }

    val secondaryColor = when (state) {
        AssistantState.IDLE -> theme.secondaryAccent
        AssistantState.LISTENING -> theme.tertiaryAccent
        AssistantState.THINKING -> theme.primaryAccent
        AssistantState.EXECUTING -> theme.primaryAccent
        AssistantState.SPEAKING -> theme.primaryAccent
        AssistantState.ERROR -> Color(0xFFFF9100)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val baseRadius = this.size.minDimension / 2
            val audioScale = (1.0f + (amplitude.coerceIn(0f, 1f) * 0.44f)) * corePulse

            when (theme.orbStyle) {
                OrbAnimationPreset.SIRI -> {
                    drawSiriEnergyOrb(
                        center = center,
                        baseRadius = baseRadius,
                        audioScale = audioScale,
                        rotation = continuousRotation,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        glowIntensity = theme.orbGlowIntensity
                    )
                }
                OrbAnimationPreset.PLASMA -> {
                    drawPlasmaCore(
                        center = center,
                        baseRadius = baseRadius,
                        audioScale = audioScale,
                        rotation = continuousRotation * 1.8f,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        glowIntensity = theme.orbGlowIntensity
                    )
                }
                OrbAnimationPreset.GALAXY -> {
                    drawGalaxyCore(
                        center = center,
                        baseRadius = baseRadius,
                        audioScale = audioScale,
                        rotation = continuousRotation,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        particleIntensity = theme.particleIntensity
                    )
                }
                OrbAnimationPreset.CYBERNETIC -> {
                    drawCyberneticOrb(
                        center = center,
                        baseRadius = baseRadius,
                        audioScale = audioScale,
                        outerRotation = continuousRotation,
                        middleRotation = reverseRotation,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
                OrbAnimationPreset.MINIMAL -> {
                    drawMinimalPulseOrb(
                        center = center,
                        baseRadius = baseRadius,
                        audioScale = audioScale,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
                OrbAnimationPreset.LIQUID -> {
                    drawLiquidGradientOrb(
                        center = center,
                        baseRadius = baseRadius,
                        audioScale = audioScale,
                        rotation = continuousRotation,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        tertiaryColor = theme.tertiaryAccent
                    )
                }
                OrbAnimationPreset.HOLOGRAPHIC -> {
                    drawHolographicArcOrb(
                        center = center,
                        baseRadius = baseRadius,
                        audioScale = audioScale,
                        rotation = continuousRotation,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        tertiaryColor = theme.tertiaryAccent
                    )
                }
            }
        }
    }
}

// 1. Siri-Inspired Energy Orb
private fun DrawScope.drawSiriEnergyOrb(
    center: Offset,
    baseRadius: Float,
    audioScale: Float,
    rotation: Float,
    primaryColor: Color,
    secondaryColor: Color,
    glowIntensity: Float
) {
    val coreR = baseRadius * 0.36f * audioScale
    // Outer atmospheric glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.5f * glowIntensity),
                secondaryColor.copy(alpha = 0.25f * glowIntensity),
                Color.Transparent
            ),
            center = center,
            radius = baseRadius * 0.95f
        ),
        radius = baseRadius * 0.95f,
        center = center
    )

    // Siri fluid organic rings
    rotate(rotation, center) {
        for (i in 0..2) {
            val ringR = coreR * (1.2f + i * 0.35f)
            drawCircle(
                color = if (i % 2 == 0) primaryColor.copy(alpha = 0.45f) else secondaryColor.copy(alpha = 0.45f),
                radius = ringR,
                center = center,
                style = Stroke(width = (2.5f - i * 0.6f).dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }

    // Inner bright white plasma core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, primaryColor, secondaryColor.copy(alpha = 0.4f), Color.Transparent),
            center = center,
            radius = coreR * 1.5f
        ),
        radius = coreR * 1.5f,
        center = center
    )
}

// 2. Plasma Core
private fun DrawScope.drawPlasmaCore(
    center: Offset,
    baseRadius: Float,
    audioScale: Float,
    rotation: Float,
    primaryColor: Color,
    secondaryColor: Color,
    glowIntensity: Float
) {
    val coreR = baseRadius * 0.38f * audioScale
    rotate(rotation, center) {
        for (step in 0 until 8) {
            val angle = Math.toRadians((step * 45).toDouble())
            val endX = center.x + (cos(angle) * baseRadius * 0.85f).toFloat()
            val endY = center.y + (sin(angle) * baseRadius * 0.85f).toFloat()
            drawLine(
                color = primaryColor.copy(alpha = 0.45f * glowIntensity),
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, primaryColor, secondaryColor, Color.Transparent),
            center = center,
            radius = coreR * 1.6f
        ),
        radius = coreR * 1.6f,
        center = center
    )
}

// 3. Galaxy Core
private fun DrawScope.drawGalaxyCore(
    center: Offset,
    baseRadius: Float,
    audioScale: Float,
    rotation: Float,
    primaryColor: Color,
    secondaryColor: Color,
    particleIntensity: Float
) {
    val coreR = baseRadius * 0.30f * audioScale
    rotate(rotation, center) {
        val stars = (24 * particleIntensity).toInt().coerceIn(12, 48)
        for (i in 0 until stars) {
            val theta = (i * 0.28f)
            val r = (coreR + i * (baseRadius * 0.65f / stars))
            val sx = center.x + (cos(theta.toDouble()) * r).toFloat()
            val sy = center.y + (sin(theta.toDouble()) * r).toFloat()
            drawCircle(
                color = if (i % 2 == 0) primaryColor else secondaryColor,
                radius = (1.5f + (i % 3) * 0.8f).dp.toPx(),
                center = Offset(sx, sy)
            )
        }
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, primaryColor, secondaryColor.copy(alpha = 0.3f), Color.Transparent),
            center = center,
            radius = coreR * 1.4f
        ),
        radius = coreR * 1.4f,
        center = center
    )
}

// 4. Cybernetic Energy HUD
private fun DrawScope.drawCyberneticOrb(
    center: Offset,
    baseRadius: Float,
    audioScale: Float,
    outerRotation: Float,
    middleRotation: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val coreR = baseRadius * 0.30f * audioScale

    // Core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, primaryColor, primaryColor.copy(alpha = 0.3f), Color.Transparent),
            center = center,
            radius = coreR * 1.5f
        ),
        radius = coreR * 1.5f,
        center = center
    )

    // Segmented ring
    rotate(middleRotation, center) {
        val midR = baseRadius * 0.58f
        for (i in 0..2) {
            drawArc(
                color = primaryColor.copy(alpha = 0.8f),
                startAngle = i * 120f,
                sweepAngle = 75f,
                useCenter = false,
                topLeft = Offset(center.x - midR, center.y - midR),
                size = Size(midR * 2, midR * 2),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }

    // Outer ticks
    rotate(outerRotation, center) {
        val outR = baseRadius * 0.88f
        drawCircle(
            color = secondaryColor.copy(alpha = 0.4f),
            radius = outR,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30).toDouble())
            val len = if (i % 3 == 0) 12.dp.toPx() else 6.dp.toPx()
            val p1 = Offset(
                (center.x + (outR - len) * cos(angle)).toFloat(),
                (center.y + (outR - len) * sin(angle)).toFloat()
            )
            val p2 = Offset(
                (center.x + outR * cos(angle)).toFloat(),
                (center.y + outR * sin(angle)).toFloat()
            )
            drawLine(
                color = primaryColor.copy(alpha = if (i % 3 == 0) 0.9f else 0.4f),
                start = p1,
                end = p2,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

// 5. Minimal Pulse
private fun DrawScope.drawMinimalPulseOrb(
    center: Offset,
    baseRadius: Float,
    audioScale: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val coreR = baseRadius * 0.32f * audioScale
    drawCircle(
        color = secondaryColor.copy(alpha = 0.15f),
        radius = baseRadius * 0.82f * audioScale,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
    drawCircle(
        color = primaryColor.copy(alpha = 0.35f),
        radius = baseRadius * 0.58f * audioScale,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, primaryColor, Color.Transparent),
            center = center,
            radius = coreR * 1.3f
        ),
        radius = coreR * 1.3f,
        center = center
    )
}

// 6. Liquid Gradient
private fun DrawScope.drawLiquidGradientOrb(
    center: Offset,
    baseRadius: Float,
    audioScale: Float,
    rotation: Float,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color
) {
    val coreR = baseRadius * 0.40f * audioScale
    rotate(rotation, center) {
        val o1 = Offset(center.x + (coreR * 0.3f), center.y - (coreR * 0.2f))
        val o2 = Offset(center.x - (coreR * 0.3f), center.y + (coreR * 0.2f))
        drawCircle(
            brush = Brush.radialGradient(listOf(primaryColor, Color.Transparent), center = o1, radius = coreR * 1.2f),
            radius = coreR * 1.2f,
            center = o1
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(secondaryColor, Color.Transparent), center = o2, radius = coreR * 1.2f),
            radius = coreR * 1.2f,
            center = o2
        )
    }
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.White, tertiaryColor, Color.Transparent), center = center, radius = coreR * 0.8f),
        radius = coreR * 0.8f,
        center = center
    )
}

// 7. Holographic Orb
private fun DrawScope.drawHolographicArcOrb(
    center: Offset,
    baseRadius: Float,
    audioScale: Float,
    rotation: Float,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color
) {
    val coreR = baseRadius * 0.36f * audioScale
    rotate(rotation, center) {
        val rainbow = listOf(primaryColor, secondaryColor, tertiaryColor, primaryColor)
        drawCircle(
            brush = Brush.sweepGradient(rainbow, center),
            radius = coreR * 1.5f,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, primaryColor.copy(alpha = 0.8f), secondaryColor.copy(alpha = 0.3f), Color.Transparent),
            center = center,
            radius = coreR * 1.3f
        ),
        radius = coreR * 1.3f,
        center = center
    )
}

@Composable
fun AudioWaveformVisualizer(
    isLive: Boolean,
    amplitude: Float,
    barCount: Int = 18,
    modifier: Modifier = Modifier,
    theme: ThemeConfig = JarvisTheme.config
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val offsetAngle = phase + (i * 0.35f)
            val dynamicWave = (sin(offsetAngle) * 0.5f + 0.5f)
            val heightFraction = if (isLive) {
                ((amplitude * 0.7f) + (dynamicWave * 0.3f)).coerceIn(0.12f, 1.0f)
            } else {
                0.08f
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .height((heightFraction * 40).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = if (isLive) {
                                listOf(theme.primaryAccent, theme.secondaryAccent)
                            } else {
                                listOf(theme.surfaceColor, theme.cardBg)
                            }
                        )
                    )
            )
        }
    }
}

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color = JarvisTheme.config.borderGlow,
    glowColor: Color = borderColor,
    backgroundColor: Color = JarvisTheme.config.cardBg,
    cornerRadius: Dp = JarvisTheme.config.cardCornerRadiusDp.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    Surface(
        modifier = clickableModifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .border(1.dp, glowColor, RoundedCornerShape(cornerRadius)),
        color = backgroundColor,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

@Composable
fun StatusBadge(
    state: AssistantState,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    theme: ThemeConfig = JarvisTheme.config
) {
    val (labelEn, labelBn, dotColor) = when (state) {
        AssistantState.IDLE -> Triple("IDLE", "JARVIS প্রস্তুত", theme.primaryAccent)
        AssistantState.LISTENING -> Triple("LISTENING", "শুনছি...", theme.primaryAccent)
        AssistantState.THINKING -> Triple("THINKING", "ভাবছি...", theme.secondaryAccent)
        AssistantState.EXECUTING -> Triple("EXECUTING", "কাজ সম্পন্ন করছি...", theme.tertiaryAccent)
        AssistantState.SPEAKING -> Triple("SPEAKING", "বলছি...", NeonEmerald)
        AssistantState.ERROR -> Triple("ERROR", "ত্রুটি ঘটেছে", RedAlert)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, dotColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        color = theme.surfaceColor.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = "$labelEn • $labelBn",
                color = theme.textPrimary,
                fontSize = 12.sp,
                style = MaterialTheme.typography.labelMedium
            )

            if (state == AssistantState.ERROR && onRetry != null) {
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "পুনরায় চেষ্টা করুন",
                        tint = theme.primaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
