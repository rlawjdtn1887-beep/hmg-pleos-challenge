package com.ecofit.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.ecofit.app.ui.theme.Ice
import com.ecofit.app.ui.theme.Ink
import com.ecofit.app.ui.theme.Paper

// 검정 배경 + 흰 e + 아이스 블루 점
@Composable
fun EcoLogo(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        drawRoundRect(Ink, cornerRadius = CornerRadius(w * 0.26f))

        val stroke = w * 0.085f
        val c = Offset(w * 0.48f, w * 0.54f)
        val r = w * 0.19f
        drawLine(Paper, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = stroke, cap = StrokeCap.Round)
        drawArc(
            Paper, startAngle = 0f, sweepAngle = -320f, useCenter = false,
            topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawCircle(Ice, radius = w * 0.08f, center = Offset(w * 0.72f, w * 0.30f))
    }
}
