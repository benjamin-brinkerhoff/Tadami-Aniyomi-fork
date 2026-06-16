package eu.kanade.presentation.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.theme.AuroraTheme
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.pow

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val uiPreferences = remember { Injekt.get<UiPreferences>() }
    val colors = AuroraTheme.colors
    val animatedAuroraBackground by uiPreferences.animatedAuroraBackground().collectAsState()
    val specialBackgroundStyle by uiPreferences.specialBackgroundStyle().collectAsState()

    AuroraAmbientBackground(
        enabled = animatedAuroraBackground && !colors.isEInk,
        specialBackgroundStyle = specialBackgroundStyle,
        modifier = modifier,
        content = content,
    )
}

@Composable
fun AuroraAmbientBackground(
    enabled: Boolean,
    specialBackgroundStyle: String = "none",
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AuroraTheme.colors
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(lifecycleOwner.lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            isLifecycleResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val shouldAnimate = shouldAnimateAuroraBackground(
        userEnabled = enabled && !colors.isEInk,
        isLifecycleResumed = isLifecycleResumed,
        systemAnimationsEnabled = ValueAnimator.areAnimatorsEnabled(),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundGradient),
    ) {
        if (enabled && !colors.isEInk) {
            if (specialBackgroundStyle == "none") {
                if (shouldAnimate) {
                    val transition = rememberInfiniteTransition(label = "auroraAmbient")
                    val blobOneDriftX by transition.animateFloat(
                        initialValue = -0.08f,
                        targetValue = 0.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 26000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobOneDriftX",
                    )
                    val blobOneDriftY by transition.animateFloat(
                        initialValue = -0.05f,
                        targetValue = 0.04f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 30000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobOneDriftY",
                    )
                    val blobTwoDriftX by transition.animateFloat(
                        initialValue = 0.08f,
                        targetValue = -0.06f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 22000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobTwoDriftX",
                    )
                    val blobTwoDriftY by transition.animateFloat(
                        initialValue = 0.02f,
                        targetValue = -0.08f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 28000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobTwoDriftY",
                    )
                    val blobThreeDriftX by transition.animateFloat(
                        initialValue = -0.03f,
                        targetValue = 0.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 20000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobThreeDriftX",
                    )
                    val blobThreeDriftY by transition.animateFloat(
                        initialValue = 0.06f,
                        targetValue = -0.04f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 24000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobThreeDriftY",
                    )
                    val blobOneAlpha by transition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 0.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 18000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobOneAlpha",
                    )
                    val blobTwoAlpha by transition.animateFloat(
                        initialValue = 0.12f,
                        targetValue = 0.22f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 20000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobTwoAlpha",
                    )
                    val blobThreeAlpha by transition.animateFloat(
                        initialValue = 0.1f,
                        targetValue = 0.18f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 24000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobThreeAlpha",
                    )

                    AuroraAmbientCanvas(
                        colors = colors,
                        blobOneDriftX = blobOneDriftX,
                        blobOneDriftY = blobOneDriftY,
                        blobTwoDriftX = blobTwoDriftX,
                        blobTwoDriftY = blobTwoDriftY,
                        blobThreeDriftX = blobThreeDriftX,
                        blobThreeDriftY = blobThreeDriftY,
                        blobOneAlpha = blobOneAlpha,
                        blobTwoAlpha = blobTwoAlpha,
                        blobThreeAlpha = blobThreeAlpha,
                    )
                } else {
                    AuroraAmbientCanvas(
                        colors = colors,
                        blobOneDriftX = -0.08f,
                        blobOneDriftY = -0.05f,
                        blobTwoDriftX = 0.08f,
                        blobTwoDriftY = 0.02f,
                        blobThreeDriftX = -0.03f,
                        blobThreeDriftY = 0.06f,
                        blobOneAlpha = 0.2f,
                        blobTwoAlpha = 0.12f,
                        blobThreeAlpha = 0.1f,
                    )
                }
            } else {
                AuroraSpecialBackgroundCanvas(
                    colors = colors,
                    styleKey = specialBackgroundStyle,
                    animate = shouldAnimate,
                )
            }
        }

        content()
    }
}

@Composable
private fun AuroraAmbientCanvas(
    colors: eu.kanade.presentation.theme.AuroraColors,
    blobOneDriftX: Float,
    blobOneDriftY: Float,
    blobTwoDriftX: Float,
    blobTwoDriftY: Float,
    blobThreeDriftX: Float,
    blobThreeDriftY: Float,
    blobOneAlpha: Float,
    blobTwoAlpha: Float,
    blobThreeAlpha: Float,
) {
    Canvas(
        modifier = Modifier.fillMaxSize(),
    ) {
        fun drawAmbientBlob(
            centerFractionX: Float,
            centerFractionY: Float,
            driftX: Float,
            driftY: Float,
            radiusFraction: Float,
            aspectX: Float,
            aspectY: Float,
            color: Color,
            alpha: Float,
        ) {
            val center = Offset(
                x = size.width * (centerFractionX + driftX),
                y = size.height * (centerFractionY + driftY),
            )
            val radius = size.minDimension * radiusFraction
            withTransform({
                translate(
                    left = center.x - radius * aspectX,
                    top = center.y - radius * aspectY,
                )
                scale(scaleX = aspectX, scaleY = aspectY, pivot = Offset.Zero)
            }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = alpha),
                            Color.Transparent,
                        ),
                        center = Offset(radius, radius),
                        radius = radius,
                    ),
                    radius = radius,
                )
            }
        }

        val blobBaseAlpha = if (colors.isDark) 1f else 0.45f
        drawAmbientBlob(
            centerFractionX = 0.22f,
            centerFractionY = 0.14f,
            driftX = blobOneDriftX,
            driftY = blobOneDriftY,
            radiusFraction = 0.5f,
            aspectX = 1.35f,
            aspectY = 0.8f,
            color = colors.glowEffect,
            alpha = blobOneAlpha * blobBaseAlpha,
        )
        drawAmbientBlob(
            centerFractionX = 0.84f,
            centerFractionY = 0.22f,
            driftX = blobTwoDriftX,
            driftY = blobTwoDriftY,
            radiusFraction = 0.44f,
            aspectX = 1.15f,
            aspectY = 0.72f,
            color = colors.gradientPurple,
            alpha = blobTwoAlpha * blobBaseAlpha,
        )
        drawAmbientBlob(
            centerFractionX = 0.48f,
            centerFractionY = 0.72f,
            driftX = blobThreeDriftX,
            driftY = blobThreeDriftY,
            radiusFraction = 0.52f,
            aspectX = 1.45f,
            aspectY = 0.9f,
            color = colors.accent,
            alpha = blobThreeAlpha * 0.72f,
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    colors.background.copy(alpha = if (colors.isDark) 0.2f else 0.12f),
                    colors.background.copy(alpha = if (colors.isDark) 0.38f else 0.2f),
                ),
                center = Offset(size.width * 0.5f, size.height * 0.4f),
                radius = size.maxDimension * 0.92f,
            ),
        )
    }
}

private data class PetalConfig(
    val initialX: Float,
    val initialY: Float,
    val speedY: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val rotationSpeed: Float,
    val scale: Float,
    val phaseOffset: Float,
)

private val PETALS = listOf(
    PetalConfig(0.10f, 0.05f, 0.22f, 0.035f, 1.4f, 40f, 0.85f, 0.0f),
    PetalConfig(0.25f, 0.40f, 0.18f, 0.045f, 1.1f, -30f, 0.70f, 1.2f),
    PetalConfig(0.40f, 0.15f, 0.26f, 0.025f, 1.8f, 60f, 1.10f, 0.5f),
    PetalConfig(0.55f, 0.75f, 0.20f, 0.040f, 1.3f, -45f, 0.95f, 2.3f),
    PetalConfig(0.70f, 0.30f, 0.24f, 0.030f, 1.6f, 50f, 0.80f, 3.1f),
    PetalConfig(0.85f, 0.60f, 0.16f, 0.050f, 0.9f, -25f, 0.65f, 0.8f),
    PetalConfig(0.95f, 0.10f, 0.28f, 0.020f, 2.0f, 70f, 1.00f, 4.2f),
    PetalConfig(0.05f, 0.80f, 0.20f, 0.040f, 1.2f, -35f, 0.75f, 1.7f),
    PetalConfig(0.18f, 0.65f, 0.25f, 0.030f, 1.5f, 55f, 0.90f, 2.8f),
    PetalConfig(0.32f, 0.90f, 0.19f, 0.045f, 1.0f, -40f, 0.75f, 0.3f),
    PetalConfig(0.48f, 0.35f, 0.23f, 0.035f, 1.3f, 45f, 1.05f, 1.9f),
    PetalConfig(0.62f, 0.50f, 0.21f, 0.040f, 1.2f, -50f, 0.80f, 3.5f),
    PetalConfig(0.78f, 0.85f, 0.17f, 0.050f, 0.8f, 30f, 0.70f, 0.9f),
    PetalConfig(0.90f, 0.45f, 0.27f, 0.025f, 1.9f, -65f, 1.15f, 2.1f),
    PetalConfig(0.12f, 0.32f, 0.21f, 0.038f, 1.4f, 35f, 0.85f, 0.6f),
    PetalConfig(0.82f, 0.02f, 0.23f, 0.032f, 1.6f, -42f, 0.95f, 1.4f),
    PetalConfig(0.22f, 0.12f, 0.24f, 0.033f, 1.5f, 48f, 0.88f, 3.7f),
    PetalConfig(0.38f, 0.58f, 0.17f, 0.048f, 0.9f, -28f, 0.72f, 2.5f),
    PetalConfig(0.52f, 0.08f, 0.27f, 0.028f, 1.7f, 58f, 1.12f, 0.1f),
    PetalConfig(0.68f, 0.95f, 0.20f, 0.042f, 1.1f, -46f, 0.92f, 1.3f),
    PetalConfig(0.74f, 0.22f, 0.25f, 0.031f, 1.6f, 52f, 0.82f, 2.9f),
    PetalConfig(0.88f, 0.70f, 0.15f, 0.052f, 0.7f, -22f, 0.62f, 4.0f),
    PetalConfig(0.98f, 0.28f, 0.29f, 0.022f, 2.1f, 72f, 1.02f, 0.7f),
    PetalConfig(0.28f, 0.82f, 0.18f, 0.042f, 1.1f, -38f, 0.78f, 3.3f),
)

private data class StarConfig(
    val xFraction: Float,
    val yFraction: Float,
    val pulseSpeed: Float,
    val phase: Float,
    val size: Float,
)

private val FLOATING_STARS = listOf(
    StarConfig(0.12f, 0.15f, 1.2f, 0.0f, 2f),
    StarConfig(0.85f, 0.25f, 0.8f, 1.5f, 3f),
    StarConfig(0.35f, 0.78f, 1.5f, 0.7f, 1.5f),
    StarConfig(0.72f, 0.65f, 1.0f, 2.3f, 2.5f),
    StarConfig(0.22f, 0.45f, 0.7f, 3.1f, 2f),
    StarConfig(0.90f, 0.82f, 1.4f, 1.1f, 3.5f),
    StarConfig(0.08f, 0.70f, 1.1f, 0.4f, 2f),
    StarConfig(0.55f, 0.12f, 1.3f, 2.8f, 1.8f),
    StarConfig(0.42f, 0.30f, 0.9f, 1.9f, 2.5f),
    StarConfig(0.68f, 0.28f, 1.6f, 0.2f, 1.5f),
    StarConfig(0.18f, 0.88f, 0.6f, 3.5f, 3f),
    StarConfig(0.80f, 0.55f, 1.2f, 2.0f, 2.2f),
    StarConfig(0.48f, 0.92f, 1.0f, 1.7f, 2.5f),
    StarConfig(0.28f, 0.22f, 1.5f, 0.9f, 1.8f),
    StarConfig(0.62f, 0.85f, 0.8f, 2.5f, 3f),
)

private fun floatMod(value: Float, max: Float): Float {
    val r = value % max
    return if (r < 0f) r + max else r
}

private fun getCometProgress(elapsed: Float, duration: Float, delay: Float): Float {
    val cycle = duration + delay
    val progress = (elapsed % cycle) - delay
    return if (progress < 0f) 0f else progress / duration
}

@Composable
private fun AuroraSpecialBackgroundCanvas(
    colors: eu.kanade.presentation.theme.AuroraColors,
    styleKey: String,
    animate: Boolean,
) {
    if (styleKey == "none" || colors.isEInk) return

    var timeMillis by remember { mutableStateOf(0L) }

    if (animate) {
        LaunchedEffect(Unit) {
            val startTime = android.os.SystemClock.uptimeMillis()
            while (true) {
                withFrameMillis { frameTime ->
                    timeMillis = frameTime - startTime
                }
            }
        }
    }

    val elapsedSeconds = timeMillis / 1000f
    // Use a slow linear spin for orbits without modulo resets to prevent teleporting/jumping
    val orbitSpin = if (animate) elapsedSeconds * 4f else 0f

    val pulseProgress = elapsedSeconds * (2f * Math.PI.toFloat() / 2.2f)
    val pulse = if (animate) 0.85f + 0.15f * kotlin.math.sin(pulseProgress) else 0.85f

    val cometProgressOne = if (animate) getCometProgress(elapsedSeconds + 1.8f, 22f, 5f) else 0f
    val cometProgressTwo = if (animate) getCometProgress(elapsedSeconds + 9.6f, 28f, 9f) else 0f

    val petalTemplatePath = remember {
        Path().apply {
            moveTo(0f, -0.5f)
            quadraticTo(0.45f, -0.28f, 0.275f, 0.1f)
            quadraticTo(0.1f, 0.5f, 0f, 0.5f)
            quadraticTo(-0.1f, 0.5f, -0.275f, 0.1f)
            quadraticTo(-0.45f, -0.28f, 0f, -0.5f)
            close()
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (styleKey == "neon_orbit" ||
            styleKey == "trinity_constellation" ||
            styleKey == "deep_space_archive"
        ) {
            FLOATING_STARS.forEach { star ->
                val alpha =
                    0.04f + 0.06f * kotlin.math.abs(kotlin.math.sin(elapsedSeconds * star.pulseSpeed + star.phase))
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = star.size.dp.toPx(),
                    center = Offset(star.xFraction * size.width, star.yFraction * size.height),
                )
            }
        }

        when (styleKey) {
            "petal_storm" -> {
                val petalColor = if (colors.isDark) {
                    Color(0xFFFFA7C8)
                } else {
                    Color(0xFFFF84B7)
                }

                val petalTime = elapsedSeconds * 0.20f
                PETALS.forEachIndexed { index, petal ->
                    val yVal = ((petal.initialY + petal.speedY * petalTime) % 1.2f) - 0.1f

                    val sway = petal.swayAmp * kotlin.math.sin(petal.swayFreq * petalTime + petal.phaseOffset)
                    val xVal = floatMod(petal.initialX + sway, 1.0f)

                    val rotationDeg = petal.rotationSpeed * petalTime + (petal.phaseOffset * 50f)
                    val scale3DX = kotlin.math.abs(
                        kotlin.math.cos(petalTime * 2.5f + petal.phaseOffset),
                    ).coerceAtLeast(0.12f)

                    val petalLength = 28f + (index % 4) * 4f
                    val petalWidth = 10f + (index % 3) * 1.8f

                    withTransform({
                        translate(
                            left = xVal * size.width,
                            top = yVal * size.height,
                        )
                        rotate(rotationDeg, pivot = Offset.Zero)
                        scale(
                            scaleX = petal.scale * scale3DX * petalWidth,
                            scaleY = petal.scale * petalLength,
                            pivot = Offset.Zero,
                        )
                    }) {
                        drawPath(
                            path = petalTemplatePath,
                            color = petalColor.copy(alpha = 0.22f + (0.06f * (index % 5))),
                        )
                    }
                }
            }
            "trinity_constellation" -> {
                val center = Offset(size.width * 0.5f, size.height * 0.42f)
                val nodeColors = listOf(Color(0xFF64E8FF), Color(0xFF9C7CFF), Color(0xFFFFD36E))

                // 2. Rotating Astrolabe Grid & Constellation
                val rotationAngle = orbitSpin * 0.4f // very slow, elegant rotation
                withTransform({
                    rotate(rotationAngle, pivot = center)
                }) {
                    // Draw Astrolabe / Celestial Coordinates Grid
                    val gridAlpha = 0.015f // extremely subtle
                    val strokeDash = PathEffect.dashPathEffect(floatArrayOf(8f, 16f), 0f)

                    // Concentric rings
                    drawCircle(
                        color = Color.White.copy(alpha = gridAlpha),
                        radius = size.minDimension * 0.22f,
                        style = Stroke(width = 0.8.dp.toPx()),
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = gridAlpha),
                        radius = size.minDimension * 0.38f,
                        style = Stroke(width = 0.8.dp.toPx(), pathEffect = strokeDash),
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = gridAlpha * 0.7f),
                        radius = size.minDimension * 0.54f,
                        style = Stroke(width = 0.6.dp.toPx()),
                    )

                    // Faint radial coordinate ticks / lines
                    repeat(8) { j ->
                        val angleRad = Math.toRadians((j * 45.0)).toFloat()
                        val cos = kotlin.math.cos(angleRad)
                        val sin = kotlin.math.sin(angleRad)
                        val startRadius = size.minDimension * 0.15f
                        val endRadius = size.minDimension * 0.58f
                        drawLine(
                            color = Color.White.copy(alpha = gridAlpha * 0.5f),
                            start = Offset(center.x + cos * startRadius, center.y + sin * startRadius),
                            end = Offset(center.x + cos * endRadius, center.y + sin * endRadius),
                            strokeWidth = 0.6.dp.toPx(),
                        )
                    }

                    val numStars = 12
                    // Draw connecting lines and traveling stardust (zero allocation)
                    repeat(numStars) { i ->
                        val star = FLOATING_STARS[i]
                        val nextStar = FLOATING_STARS[(i + 3) % numStars]
                        val color = nodeColors[i % nodeColors.size]

                        val startX = star.xFraction * size.width
                        val startY = star.yFraction * size.height
                        val endX = nextStar.xFraction * size.width
                        val endY = nextStar.yFraction * size.height

                        val lineAlpha = 0.02f + 0.02f * kotlin.math.abs(kotlin.math.sin(elapsedSeconds * 1.5f + i))

                        drawLine(
                            color = color.copy(alpha = lineAlpha),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 0.8.dp.toPx(),
                        )

                        // Traveling stardust particle along the line
                        val travelProgress = (elapsedSeconds * 0.08f + i * 0.15f) % 1.0f
                        val pX = startX + (endX - startX) * travelProgress
                        val pY = startY + (endY - startY) * travelProgress
                        drawCircle(
                            color = Color.White.copy(alpha = 0.14f),
                            radius = 1.2.dp.toPx(),
                            center = Offset(pX, pY),
                        )
                    }

                    // Draw star nodes (with twinkle effect)
                    repeat(numStars) { i ->
                        val star = FLOATING_STARS[i]
                        val color = nodeColors[i % nodeColors.size]
                        val startX = star.xFraction * size.width
                        val startY = star.yFraction * size.height

                        val twinkle = 0.5f + 0.5f * kotlin.math.sin(elapsedSeconds * 2.5f + i)
                        val outerRadius = (2.2f + (i % 3) * 0.6f + twinkle * 0.8f).dp.toPx()

                        drawCircle(
                            color = color.copy(alpha = 0.08f * twinkle),
                            radius = outerRadius * 1.6f,
                            center = Offset(startX, startY),
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f + 0.10f * twinkle),
                            radius = 1.0.dp.toPx(),
                            center = Offset(startX, startY),
                        )
                    }
                }

                // 3. Subtle shooting stars (not rotated, they dash across the screen naturally)
                fun drawSubtleComet(
                    progress: Float,
                    start: Offset,
                    end: Offset,
                    color: Color,
                ) {
                    val visibility = when {
                        progress < 0.08f -> progress / 0.08f
                        progress < 0.18f -> 1f - ((progress - 0.08f) / 0.10f)
                        else -> 0f
                    }
                    if (visibility <= 0f) return

                    val head = Offset(
                        x = start.x + ((end.x - start.x) * progress),
                        y = start.y + ((end.y - start.y) * progress),
                    )
                    val tailLength = size.minDimension * (0.12f + (0.03f * progress))
                    val tailDirection = Offset(
                        x = -(end.x - start.x),
                        y = -(end.y - start.y),
                    )
                    val magnitude = kotlin.math.sqrt(
                        (tailDirection.x * tailDirection.x) + (tailDirection.y * tailDirection.y),
                    ).coerceAtLeast(1f)
                    val tailUnit = Offset(
                        x = tailDirection.x / magnitude,
                        y = tailDirection.y / magnitude,
                    )
                    val tail = Offset(
                        x = head.x + (tailUnit.x * tailLength),
                        y = head.y + (tailUnit.y * tailLength),
                    )

                    drawLine(
                        color = color.copy(alpha = 0.015f * visibility),
                        start = tail,
                        end = head,
                        strokeWidth = 1.2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.025f * visibility),
                        radius = 1.2.dp.toPx(),
                        center = head,
                    )
                }

                drawSubtleComet(
                    progress = cometProgressOne,
                    start = Offset(-size.width * 0.1f, size.height * 0.15f),
                    end = Offset(size.width * 1.1f, size.height * 0.75f),
                    color = Color(0xFFBDFBFF),
                )
                drawSubtleComet(
                    progress = cometProgressTwo,
                    start = Offset(-size.width * 0.05f, size.height * 0.8f),
                    end = Offset(size.width * 1.05f, size.height * 0.2f),
                    color = Color(0xFF9B8CFF),
                )
            }
            "deep_space_archive" -> {
                // 1. Classical Temple Arch & Columns (Vector outlines from Concept 3)
                // Left and right column anchor X positions
                val xLeft = size.width * 0.15f
                val xRight = size.width * 0.85f
                val columnAlpha = 0.012f
                val columnColor = colors.accent.copy(alpha = columnAlpha)

                // Left Column Fluting Lines
                drawLine(
                    columnColor,
                    Offset(xLeft - 8.dp.toPx(), 0f),
                    Offset(xLeft - 8.dp.toPx(), size.height),
                    0.6.dp.toPx(),
                )
                drawLine(
                    colors.glowEffect.copy(alpha = columnAlpha * 1.5f),
                    Offset(xLeft, 0f),
                    Offset(xLeft, size.height),
                    1.0.dp.toPx(),
                )
                drawLine(
                    columnColor,
                    Offset(xLeft + 8.dp.toPx(), 0f),
                    Offset(xLeft + 8.dp.toPx(), size.height),
                    0.6.dp.toPx(),
                )

                // Right Column Fluting Lines
                drawLine(
                    columnColor,
                    Offset(xRight - 8.dp.toPx(), 0f),
                    Offset(xRight - 8.dp.toPx(), size.height),
                    0.6.dp.toPx(),
                )
                drawLine(
                    colors.glowEffect.copy(alpha = columnAlpha * 1.5f),
                    Offset(xRight, 0f),
                    Offset(xRight, size.height),
                    1.0.dp.toPx(),
                )
                drawLine(
                    columnColor,
                    Offset(xRight + 8.dp.toPx(), 0f),
                    Offset(xRight + 8.dp.toPx(), size.height),
                    0.6.dp.toPx(),
                )

                // Classical Nested Arches linking the columns
                repeat(2) { idx ->
                    val offsetFactor = 12.dp.toPx() * idx
                    val startY = size.height * 0.32f + offsetFactor
                    val controlY = -size.height * 0.12f + offsetFactor
                    val archPath = Path().apply {
                        moveTo(xLeft - 10.dp.toPx(), startY)
                        quadraticTo(size.width * 0.5f, controlY, xRight + 10.dp.toPx(), startY)
                    }
                    drawPath(
                        path = archPath,
                        color = colors.accent.copy(alpha = columnAlpha * (1.8f - idx * 0.6f)),
                        style = Stroke(width = 0.8.dp.toPx()),
                    )
                }

                // 2. Morphing Constellation Codex Particle System (Concept 4)
                val center = Offset(size.width * 0.5f, size.height * 0.45f)

                // Morph progress: 0.0 (random cosmic dust) to 1.0 (open book outline)
                // Period is 16 seconds (smoothly gathers and dissolves)
                val morphProgress = 0.5f + 0.5f * kotlin.math.sin(elapsedSeconds * 0.38f)

                val numParticles = 24
                repeat(numParticles) { i ->
                    val star = FLOATING_STARS[i % FLOATING_STARS.size]

                    // Cloud coordinate (ambient drift)
                    val driftY = floatMod(star.yFraction - elapsedSeconds * 0.012f * (1f + (i % 3) * 0.3f), 1.0f)
                    val cloudX = star.xFraction * size.width
                    val cloudY = driftY * size.height

                    // Book coordinate (parametric mapping)
                    val u = (i % 4) / 3.0f
                    val v = (i / 4) / 5.0f
                    val isRight = (i % 2 == 0)

                    val bookWidth = size.width * 0.22f
                    val bookHeight = size.height * 0.12f
                    val wave = kotlin.math.sin(u * Math.PI).toFloat() * 12.dp.toPx()

                    val bookX = if (isRight) {
                        center.x + (0.04f + u * 0.32f) * bookWidth
                    } else {
                        center.x - (0.04f + u * 0.32f) * bookWidth
                    }
                    val bookY = center.y - (bookHeight * 0.5f) + v * bookHeight - wave * (1f - v * 0.2f)

                    // Interpolated coordinate
                    val currentX = cloudX + (bookX - cloudX) * morphProgress
                    val currentY = cloudY + (bookY - cloudY) * morphProgress

                    // Colors tied strictly to active theme tokens (no hardcoding)
                    val baseColor = if (i % 2 == 0) colors.accent else colors.glowEffect
                    val alpha = (0.02f + 0.02f * (i % 3)) * (0.7f + 0.3f * kotlin.math.sin(elapsedSeconds * 3.5f + i))

                    // Draw particle core
                    drawCircle(
                        color = baseColor.copy(alpha = alpha * 1.5f),
                        radius = (1.2f + (i % 3) * 0.4f).dp.toPx(),
                        center = Offset(currentX, currentY),
                    )

                    // Subtle glowing halo
                    drawCircle(
                        color = baseColor.copy(alpha = alpha * 0.4f),
                        radius = (3.5f + (i % 3) * 0.8f).dp.toPx(),
                        center = Offset(currentX, currentY),
                    )
                }
            }
            "shadow_realm" -> {
                val center = Offset(size.width * 0.5f, size.height * 0.50f)
                val minDim = size.minDimension

                // PRESET_V4 selected in the HTML prototype:
                // { hole=0.71, disk=1.74, lens=1.08, thick=0.29, bright=0.83,
                //   ring=0.73, particles=44, particleSpeed=0.06, stars=0.9, pull=0.57, speed=0.88 }
                val holeRadius = minDim * 0.112f
                val diskRadius = minDim * 0.285f
                val lensHeight = holeRadius * 1.18f * 1.35f
                val diskThickness = holeRadius * 0.34f
                val diskBrightness = 0.96f
                val ringStrength = 0.92f
                val particleCount = 52
                val particleSpeed = 0.06f
                val starDensity = 1.85f
                val gravityPull = 0.57f
                val realmSpeed = 0.88f
                val time = if (animate) elapsedSeconds else 0f

                val voidBlack = Color.Black
                val photonWhite = Color(0xFFEAFDFF)
                val ghostLight = colors.glowEffect
                val shadowViolet = colors.gradientPurple
                val realmAccent = colors.accent

                fun srHash(seed: Float): Float {
                    return kotlin.math.abs(
                        kotlin.math.sin(seed * 127.1f + 311.7f) * 43758.5453f,
                    ) % 1f
                }

                fun srMix(a: Float, b: Float, t: Float): Float = a + (b - a) * t

                fun shadowRealmColor(t: Float, alpha: Float): Color {
                    return when {
                        t < 0.18f -> photonWhite.copy(alpha = alpha)
                        t < 0.52f -> ghostLight.copy(alpha = alpha)
                        t < 0.78f -> shadowViolet.copy(alpha = alpha)
                        else -> realmAccent.copy(alpha = alpha)
                    }
                }

                fun makeLensPath(radius: Float, height: Float, sign: Float): Path {
                    return Path().apply {
                        moveTo(center.x - radius, center.y)
                        cubicTo(
                            center.x - radius * 0.62f,
                            center.y + sign * height * 0.95f,
                            center.x - radius * 0.34f,
                            center.y + sign * height * 1.12f,
                            center.x,
                            center.y + sign * height * 1.08f,
                        )
                        cubicTo(
                            center.x + radius * 0.34f,
                            center.y + sign * height * 1.12f,
                            center.x + radius * 0.62f,
                            center.y + sign * height * 0.95f,
                            center.x + radius,
                            center.y,
                        )
                    }
                }

                // 1) Shadow Realm void glow: nearly black, with a faint violet/cyan gravitational aura.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ghostLight.copy(alpha = 0.18f * pulse),
                            shadowViolet.copy(alpha = 0.085f),
                            realmAccent.copy(alpha = 0.040f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = holeRadius * 5.2f,
                    ),
                    radius = holeRadius * 5.2f,
                    center = center,
                )

                // 2) Background stars: 2D orbital swirl around the hole, opposite to local particles.
                val backgroundStarCount = (42 * starDensity).toInt().coerceAtLeast(24)
                repeat(backgroundStarCount) { i ->
                    val seed = i + 1f
                    val baseRadius = minDim * srMix(0.18f, 0.78f, kotlin.math.sqrt(srHash(seed * 1.91f)))
                    val theta0 = srHash(seed * 3.17f) * 2f * Math.PI.toFloat()
                    val near = (holeRadius * 3.6f / (baseRadius + 1f)).coerceIn(0f, 1f)
                    val omega = (0.025f + near * 0.18f) * realmSpeed * gravityPull
                    val theta = theta0 + time * omega
                    val breathe = 1f - gravityPull * near * 0.08f *
                        (0.6f + 0.4f * kotlin.math.sin(time * 0.6f + seed))
                    val x = center.x + kotlin.math.cos(theta) * baseRadius * breathe * (1f + near * 0.18f * gravityPull)
                    val y = center.y + kotlin.math.sin(theta) * baseRadius * breathe * (1f + near * 0.18f * gravityPull)
                    val starAlpha = (0.08f + 0.16f * srHash(seed * 5.4f)) * (0.75f + near * 0.65f)
                    val starColor = when {
                        i % 11 == 0 -> realmAccent.copy(alpha = starAlpha * 0.65f)
                        i % 5 == 0 -> shadowViolet.copy(alpha = starAlpha * 0.8f)
                        else -> ghostLight.copy(alpha = starAlpha)
                    }

                    withTransform({ rotate(theta * 57.29578f + 90f, Offset(x, y)) }) {
                        drawOval(
                            color = starColor,
                            topLeft = Offset(x - (0.9f + near * 2.2f).dp.toPx(), y - 0.45f.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(
                                width = (2.3f + near * 5.2f).dp.toPx(),
                                height = 1.05f.dp.toPx(),
                            ),
                        )
                    }
                }

                // 3) Continuous lensed upper and lower arcs. No segmented rings.
                val upperArcBrush = Brush.linearGradient(
                    colors = listOf(
                        realmAccent.copy(alpha = 0.02f),
                        shadowViolet.copy(alpha = 0.12f * diskBrightness),
                        ghostLight.copy(alpha = 0.24f * diskBrightness),
                        photonWhite.copy(alpha = 0.34f * diskBrightness),
                        ghostLight.copy(alpha = 0.22f * diskBrightness),
                        shadowViolet.copy(alpha = 0.12f * diskBrightness),
                        realmAccent.copy(alpha = 0.03f),
                    ),
                    start = Offset(center.x - diskRadius, center.y - lensHeight),
                    end = Offset(center.x + diskRadius, center.y - lensHeight),
                )
                val lowerArcBrush = Brush.linearGradient(
                    colors = listOf(
                        realmAccent.copy(alpha = 0.015f),
                        shadowViolet.copy(alpha = 0.055f * diskBrightness),
                        ghostLight.copy(alpha = 0.12f * diskBrightness),
                        shadowViolet.copy(alpha = 0.06f * diskBrightness),
                        realmAccent.copy(alpha = 0.018f),
                    ),
                    start = Offset(center.x - diskRadius * 0.78f, center.y + lensHeight * 0.45f),
                    end = Offset(center.x + diskRadius * 0.78f, center.y + lensHeight * 0.45f),
                )

                repeat(8) { layer ->
                    val t = layer / 7f
                    val radius = diskRadius * srMix(0.58f, 1.02f, t)
                    val height = lensHeight * srMix(0.46f, 1.02f, t)
                    val strokeWidth = diskThickness * srMix(5.2f, 0.9f, t)
                    val alpha = diskBrightness * srMix(0.13f, 0.045f, t)
                    drawPath(
                        path = makeLensPath(radius, height, -1f),
                        brush = upperArcBrush,
                        alpha = alpha,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }

                repeat(5) { layer ->
                    val t = layer / 4f
                    val radius = diskRadius * srMix(0.48f, 0.78f, t)
                    val height = lensHeight * srMix(0.22f, 0.50f, t)
                    val strokeWidth = diskThickness * srMix(3.4f, 0.8f, t)
                    val alpha = diskBrightness * srMix(0.07f, 0.025f, t)
                    drawPath(
                        path = makeLensPath(radius, height, 1f),
                        brush = lowerArcBrush,
                        alpha = alpha,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }

                // 4) Thin continuous accretion plane through the center.
                repeat(18) { i ->
                    val yNorm = i / 17f - 0.5f
                    val alphaShape = (1f - kotlin.math.abs(yNorm) / 0.9f).coerceIn(0f, 1f)
                    if (alphaShape > 0f) {
                        val y = center.y + yNorm * diskThickness * 1.55f
                        val halfWidth = diskRadius * srMix(1.15f, 1.9f, alphaShape.pow(0.25f))
                        val diskBrush = Brush.linearGradient(
                            colors = listOf(
                                realmAccent.copy(alpha = 0f),
                                realmAccent.copy(alpha = 0.05f * diskBrightness * alphaShape),
                                ghostLight.copy(alpha = 0.15f * diskBrightness * alphaShape),
                                photonWhite.copy(alpha = 0.42f * diskBrightness * alphaShape),
                                ghostLight.copy(alpha = 0.16f * diskBrightness * alphaShape),
                                realmAccent.copy(alpha = 0.05f * diskBrightness * alphaShape),
                                realmAccent.copy(alpha = 0f),
                            ),
                            start = Offset(center.x - halfWidth, y),
                            end = Offset(center.x + halfWidth, y),
                        )
                        val wobble = kotlin.math.sin(i * 0.7f + time * realmSpeed * 0.9f) * 0.55f.dp.toPx()
                        drawLine(
                            brush = diskBrush,
                            start = Offset(center.x - halfWidth, y + wobble),
                            end = Offset(center.x + halfWidth, y - wobble),
                            strokeWidth = (0.7f + 1.5f * alphaShape).dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }

                // 5) Local particles: true 2D dots orbiting the photon ring, opposite to background stars.
                repeat(particleCount) { i ->
                    val seed = i + 1f
                    val band = srHash(seed * 9.1f)
                    val orbitRadius = holeRadius * srMix(1.18f, 2.05f, band.pow(0.85f))
                    val near = (1f - (orbitRadius - holeRadius * 1.18f) / (holeRadius * 0.95f)).coerceIn(0f, 1f)
                    val omega = (0.85f + near * 2.7f) * particleSpeed * gravityPull
                    val theta = srHash(seed * 3.7f) * 2f * Math.PI.toFloat() - time * omega
                    val x = center.x + orbitRadius * kotlin.math.cos(theta)
                    val y = center.y + orbitRadius * kotlin.math.sin(theta)
                    if (kotlin.math.hypot(x - center.x, y - center.y) > holeRadius * 1.08f) {
                        val depth = 0.72f + 0.28f * kotlin.math.sin(theta)
                        val alpha = (0.20f + 0.42f * near) * gravityPull * depth
                        val particleColor = when {
                            i % 9 == 0 -> realmAccent.copy(alpha = alpha * 0.72f)
                            i % 4 == 0 -> shadowViolet.copy(alpha = alpha * 0.82f)
                            else -> ghostLight.copy(alpha = alpha)
                        }
                        val dotRadius = (0.65f + near * 0.75f + srHash(seed * 5.3f) * 0.55f).dp.toPx()
                        drawCircle(
                            color = particleColor.copy(alpha = particleColor.alpha * 0.28f),
                            radius = dotRadius * 2.6f,
                            center = Offset(x, y),
                        )
                        drawCircle(
                            color = particleColor,
                            radius = dotRadius,
                            center = Offset(x, y),
                        )
                        drawCircle(
                            color = photonWhite.copy(alpha = alpha * 0.35f),
                            radius = dotRadius * 0.38f,
                            center = Offset(x, y),
                        )
                    }
                }

                // 6) Event horizon: real black center with soft shadow edge.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            voidBlack,
                            voidBlack,
                            voidBlack.copy(alpha = 0.92f),
                            Color.Transparent,
                        ),
                        center = Offset(center.x - holeRadius * 0.18f, center.y - holeRadius * 0.08f),
                        radius = holeRadius * 1.34f,
                    ),
                    radius = holeRadius * 1.34f,
                    center = center,
                )
                drawCircle(
                    color = voidBlack,
                    radius = holeRadius * 0.91f,
                    center = center,
                )

                // 7) Continuous photon ring + secondary rim.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            photonWhite.copy(alpha = 0.34f * ringStrength * pulse),
                            ghostLight.copy(alpha = 0.22f * ringStrength),
                            shadowViolet.copy(alpha = 0.10f * ringStrength),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = holeRadius * 1.36f,
                    ),
                    radius = holeRadius * 1.36f,
                    center = center,
                )
                drawCircle(
                    color = photonWhite.copy(alpha = 0.55f * ringStrength * pulse),
                    radius = holeRadius * 1.045f,
                    center = center,
                    style = Stroke(width = 1.25f.dp.toPx()),
                )
                drawCircle(
                    color = ghostLight.copy(alpha = 0.26f * ringStrength),
                    radius = holeRadius * 1.17f,
                    center = center,
                    style = Stroke(width = 0.85f.dp.toPx()),
                )

                // 8) A very thin front disk glint, so the disk reads as passing across the horizon.
                val frontHalfWidth = diskRadius * 1.75f
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            ghostLight.copy(alpha = 0.06f * diskBrightness),
                            photonWhite.copy(alpha = 0.13f * diskBrightness),
                            ghostLight.copy(alpha = 0.06f * diskBrightness),
                            Color.Transparent,
                        ),
                        start = Offset(center.x - frontHalfWidth, center.y),
                        end = Offset(center.x + frontHalfWidth, center.y),
                    ),
                    start = Offset(center.x - frontHalfWidth, center.y),
                    end = Offset(center.x + frontHalfWidth, center.y),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            "neon_orbit" -> {
                val ringColors = listOf(
                    Color(0xFF49E6FF),
                    Color(0xFF9B8CFF),
                    colors.accent,
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF9B8CFF).copy(alpha = 0.02f * pulse), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.15f),
                        radius = size.minDimension * 0.8f,
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.accent.copy(alpha = 0.02f * pulse), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.85f),
                        radius = size.minDimension * 0.8f,
                    ),
                )

                val center = Offset(size.width * 0.5f, size.height * 0.45f)
                val strokeEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)

                for (i in 0..2) {
                    val baseRadius = size.minDimension * (0.20f + i * 0.09f)

                    drawCircle(
                        color = Color.White.copy(alpha = 0.025f),
                        radius = baseRadius,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = strokeEffect),
                    )

                    val colorsList = listOf(
                        ringColors[i].copy(alpha = 0.12f),
                        ringColors[i].copy(alpha = 0.04f),
                        ringColors[i].copy(alpha = 0.005f),
                        Color.Transparent,
                        Color.Transparent,
                        ringColors[i].copy(alpha = 0.04f),
                        ringColors[i].copy(alpha = 0.12f),
                    )
                    val sweepBrush = Brush.sweepGradient(colors = colorsList, center = center)
                    val rotationDir = if (i % 2 == 0) 1f else -1f

                    withTransform({
                        rotate(orbitSpin * (1f + i * 0.25f) * rotationDir * 1.5f, pivot = center)
                    }) {
                        drawCircle(
                            brush = sweepBrush,
                            radius = baseRadius,
                            style = Stroke(width = 2.2.dp.toPx()),
                        )
                    }
                }

                val r0 = size.minDimension * 0.20f
                val r1 = size.minDimension * 0.29f
                val r2 = size.minDimension * 0.38f

                val angle0 = Math.toRadians(orbitSpin * 0.8 + 0.0)
                val p0 = Offset(
                    center.x + kotlin.math.cos(angle0).toFloat() * r0,
                    center.y + kotlin.math.sin(angle0).toFloat() * r0,
                )

                val angle1 = Math.toRadians(-orbitSpin * 0.5 + 60.0)
                val p1 = Offset(
                    center.x + kotlin.math.cos(angle1).toFloat() * r1,
                    center.y + kotlin.math.sin(angle1).toFloat() * r1,
                )

                val angle2 = Math.toRadians(orbitSpin * 0.4 + 120.0)
                val p2 = Offset(
                    center.x + kotlin.math.cos(angle2).toFloat() * r2,
                    center.y + kotlin.math.sin(angle2).toFloat() * r2,
                )

                val angle3 = Math.toRadians(orbitSpin * 0.7 + 180.0)
                val p3 = Offset(
                    center.x + kotlin.math.cos(angle3).toFloat() * r1,
                    center.y + kotlin.math.sin(angle3).toFloat() * r1,
                )

                val angle4 = Math.toRadians(-orbitSpin * 0.6 + 240.0)
                val p4 = Offset(
                    center.x + kotlin.math.cos(angle4).toFloat() * r2,
                    center.y + kotlin.math.sin(angle4).toFloat() * r2,
                )

                val angle5 = Math.toRadians(-orbitSpin * 0.9 + 300.0)
                val p5 = Offset(
                    center.x + kotlin.math.cos(angle5).toFloat() * r0,
                    center.y + kotlin.math.sin(angle5).toFloat() * r0,
                )

                val nodes = listOf(p0, p1, p2, p3, p4, p5)
                val nodeColors = listOf(
                    Color(0xFF49E6FF),
                    Color(0xFF9B8CFF),
                    colors.accent,
                    Color(0xFF9B8CFF),
                    colors.accent,
                    Color(0xFF49E6FF),
                )
                val nodeSizes = listOf(4.5f, 5.5f, 6.5f, 5.0f, 6.0f, 4.0f)

                val maxDist = size.minDimension * 0.35f
                for (j in 0 until nodes.size) {
                    for (k in j + 1 until nodes.size) {
                        val dx = nodes[j].x - nodes[k].x
                        val dy = nodes[j].y - nodes[k].y
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                        if (dist < maxDist) {
                            val alpha = (1f - dist / maxDist) * 0.06f * pulse
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(nodeColors[j], nodeColors[k]),
                                    start = nodes[j],
                                    end = nodes[k],
                                ),
                                start = nodes[j],
                                end = nodes[k],
                                strokeWidth = 1.dp.toPx(),
                                alpha = alpha,
                            )
                        }
                    }
                }

                for (j in 0 until nodes.size) {
                    drawCircle(
                        color = nodeColors[j].copy(alpha = 0.15f * pulse),
                        radius = (nodeSizes[j] * 1.8f).dp.toPx(),
                        center = nodes[j],
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = nodeSizes[j].dp.toPx(),
                        center = nodes[j],
                    )
                }

                fun drawComet(
                    progress: Float,
                    start: Offset,
                    end: Offset,
                    color: Color,
                ) {
                    val visibility = when {
                        progress < 0.08f -> progress / 0.08f
                        progress < 0.15f -> 1f - ((progress - 0.08f) / 0.07f)
                        else -> 0f
                    }
                    if (visibility <= 0f) return

                    val head = Offset(
                        x = start.x + ((end.x - start.x) * progress),
                        y = start.y + ((end.y - start.y) * progress),
                    )
                    val tailLength = size.minDimension * (0.16f + (0.04f * progress))
                    val tailDirection = Offset(
                        x = -(end.x - start.x),
                        y = -(end.y - start.y),
                    )
                    val magnitude = kotlin.math.sqrt(
                        (tailDirection.x * tailDirection.x) + (tailDirection.y * tailDirection.y),
                    ).coerceAtLeast(1f)
                    val tailUnit = Offset(
                        x = tailDirection.x / magnitude,
                        y = tailDirection.y / magnitude,
                    )
                    val tail = Offset(
                        x = head.x + (tailUnit.x * tailLength),
                        y = head.y + (tailUnit.y * tailLength),
                    )

                    drawLine(
                        color = color.copy(alpha = 0.02f + (0.04f * visibility)),
                        start = tail,
                        end = head,
                        strokeWidth = 1.8f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.02f * visibility),
                        start = Offset(
                            x = tail.x + (tailUnit.x * tailLength * 0.22f),
                            y = tail.y + (tailUnit.y * tailLength * 0.22f),
                        ),
                        end = head,
                        strokeWidth = 1f,
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.04f * visibility),
                        radius = 2.5f,
                        center = head,
                    )
                }

                drawComet(
                    progress = cometProgressOne,
                    start = Offset(-size.width * 0.12f, size.height * 0.18f),
                    end = Offset(size.width * 1.08f, size.height * 0.76f),
                    color = Color(0xFFBDFBFF),
                )
                drawComet(
                    progress = cometProgressTwo,
                    start = Offset(-size.width * 0.08f, size.height * 0.82f),
                    end = Offset(size.width * 1.1f, size.height * 0.14f),
                    color = Color(0xFF9B8CFF),
                )
            }
        }
    }
}

internal fun shouldAnimateAuroraBackground(
    userEnabled: Boolean,
    isLifecycleResumed: Boolean,
    systemAnimationsEnabled: Boolean,
): Boolean {
    return userEnabled && isLifecycleResumed && systemAnimationsEnabled
}

@Composable
fun AuroraHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    // Reusable header component if needed, currently TopBars are custom per screen
}
