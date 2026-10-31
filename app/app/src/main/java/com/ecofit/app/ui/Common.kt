package com.ecofit.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecofit.app.ui.theme.Mist

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
    ) { content() }
}

@Composable
fun Label(text: String) {
    Text(text, color = Mist, fontSize = 16.sp)
}

@Composable
fun BigNumber(value: String, unit: String = "") {
    Text(
        buildString { append(value); if (unit.isNotEmpty()) append(" "); append(unit) },
        fontSize = 56.sp, fontWeight = FontWeight.Bold, lineHeight = 60.sp,
    )
}

@Composable
fun ScreenTitle(text: String) {
    Text(text, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(20.dp))
}

fun Float.fmt(digits: Int = 1): String = "%.${digits}f".format(this)
fun Int.won(): String = "%,d원".format(this)
