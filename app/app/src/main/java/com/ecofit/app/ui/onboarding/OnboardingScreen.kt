package com.ecofit.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecofit.app.ui.EcoLogo
import com.ecofit.app.ui.theme.Black
import com.ecofit.app.ui.theme.Ice
import com.ecofit.app.ui.theme.Ink
import com.ecofit.app.ui.theme.Muted
import com.ecofit.app.ui.theme.Paper

// 앱을 처음 켤 때 한 번만 홈 위에 덮인다.
// 심사위원이 앱을 켠 지 5초 안에 컨셉을 이해하게 하는 게 목적이다. 그 뒤 화면들은 설명 없이 읽혀야 한다.
@Composable
fun OnboardingScreen(onStart: () -> Unit, onDemo: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Black.copy(alpha = 0.94f))
            .clickable(enabled = false) {}, // 뒤 화면 클릭 차단용
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 640.dp).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EcoLogo(84.dp)
            Spacer(Modifier.height(28.dp))
            Text(
                "가속 페달을 떼면\n돈이 쌓여요",
                fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = Paper,
                textAlign = TextAlign.Center, lineHeight = 64.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "페달을 떼면 차가 전기를 다시 채워요 (회생 제동)\n그 전기를 돈으로 보여주고, 급제동을 줄이도록 코칭해요",
                fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Muted,
                textAlign = TextAlign.Center, lineHeight = 34.sp,
            )
            Spacer(Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Ice)
                        .clickable(onClick = onStart)
                        .padding(horizontal = 36.dp, vertical = 20.dp),
                ) {
                    Text("시작하기", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Ink)
                        .clickable(onClick = onDemo)
                        .padding(horizontal = 36.dp, vertical = 20.dp),
                ) {
                    Text("▶ 데모 보기", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ice)
                }
            }
        }
    }
}
