package com.ingray.deadlock.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ingray.deadlock.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glowColor: Color = NeonCyan,
    cornerRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(SurfaceDark.copy(alpha = 0.7f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.3f),
                        glowColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .drawBehind {
                // Subtle inner glow
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.03f), Color.Transparent),
                        center = Offset(size.width / 2, 0f),
                        radius = size.width
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx())
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
fun NeonLabel(
    text: String,
    color: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
                .blur(2.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            color = color.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    valueColor: Color = NeonCyan,
    icon: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Text(text = icon, fontSize = 20.sp, modifier = Modifier.padding(bottom = 4.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = valueColor,
            letterSpacing = (-1).sp
        )
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            letterSpacing = 1.5.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DisciplineScoreNebula(
    score: Int,
    modifier: Modifier = Modifier
) {
    val color = when {
        score >= 80 -> NeonGreen
        score >= 50 -> NeonAmber
        else -> NeonRed
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "nebula")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(40.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.15f * pulse), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        androidx.compose.foundation.Canvas(modifier = Modifier.size(160.dp)) {
            val strokeWidth = 12.dp.toPx()
            
            // Background Track
            drawArc(
                color = SurfaceElevated,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Progress Track
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(color.copy(alpha = 0.1f), color, color.copy(alpha = 0.1f))
                ),
                startAngle = -90f,
                sweepAngle = 360f * (score / 100f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                color = color,
                letterSpacing = (-2).sp
            )
            Text(
                text = "DISCIPLINE",
                fontSize = 11.sp,
                letterSpacing = 4.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MiniBarChart(
    data: List<Pair<String, Int>>,
    maxValue: Int,
    barColor: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    val actualMax = maxValue.coerceAtLeast(1)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (label, value) ->
            val fraction = (value.toFloat() / actualMax).coerceIn(0f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceAtLeast(0.05f))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(barColor, barColor.copy(alpha = 0.3f))
                                )
                            )
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
