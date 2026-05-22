package com.ingray.deadlock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ingray.deadlock.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glowColor: Color = NeonCyan,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(SurfaceDark.copy(alpha = 0.8f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.4f),
                        glowColor.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
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
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 3.sp,
        color = color.copy(alpha = 0.8f),
        modifier = modifier
    )
}

@Composable
fun StatItem(
    label: String,
    value: String,
    valueColor: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DisciplineScoreRing(
    score: Int,
    modifier: Modifier = Modifier
) {
    val color = when {
        score >= 80 -> NeonGreen
        score >= 50 -> NeonAmber
        else -> NeonRed
    }
    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius = size.minDimension / 2f - strokeWidth / 2f
            drawArc(
                color = color.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * (score / 100f),
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = "SCORE",
                fontSize = 8.sp,
                letterSpacing = 2.sp,
                color = TextSecondary
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (label, value) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val fraction = (value.toFloat() / actualMax).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(barColor, barColor.copy(alpha = 0.4f))
                                )
                            )
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = TextSecondary,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}
