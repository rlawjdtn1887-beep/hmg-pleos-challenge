package com.ecofit.app

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ecofit.app.ui.EcoLogo
import com.ecofit.app.ui.briefing.BriefingScreen
import com.ecofit.app.ui.drive.DriveScreen
import com.ecofit.app.ui.home.HomeScreen
import com.ecofit.app.ui.report.ReportScreen
import com.ecofit.app.ui.theme.Black
import com.ecofit.app.ui.theme.EcoFitTheme
import com.ecofit.app.ui.theme.Ink
import com.ecofit.app.ui.theme.InkMuted
import com.ecofit.app.ui.theme.Paper

class MainActivity : ComponentActivity() {

    // CAR_SPEED는 런타임 권한. 허용되면 차속 구독을 다시 시도한다
    private val speedPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) EcoFitApp.instance.vehicleRepo.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(CAR_SPEED) != PackageManager.PERMISSION_GRANTED) {
            speedPermission.launch(CAR_SPEED)
        }
        setContent { EcoFitTheme { EcoFitNav() } }
    }

    private companion object {
        const val CAR_SPEED = "android.car.permission.CAR_SPEED"
    }
}

private data class Dest(val route: String, val label: String)

private val dests = listOf(
    Dest("home", "홈"),
    Dest("briefing", "출발 전"),
    Dest("drive", "주행 중"),
    Dest("report", "리포트"),
)

@Composable
fun EcoFitNav() {
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route

    Row(
        Modifier.fillMaxSize().background(Black).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            Modifier
                .width(270.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(Paper)
                .padding(horizontal = 14.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.padding(start = 10.dp, bottom = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EcoLogo(60.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("EcoFit", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text("회생 제동 코치", fontSize = 19.sp, color = InkMuted)
                }
            }
            dests.forEach { d ->
                val selected = current == d.route
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) Ink else Color.Transparent)
                        .clickable(role = Role.Tab) {
                            if (!selected) nav.navigate(d.route) {
                                launchSingleTop = true
                                popUpTo("home")
                            }
                        }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        d.label,
                        fontSize = 30.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (selected) Paper else InkMuted,
                    )
                }
            }
        }

        NavHost(nav, startDestination = "home", modifier = Modifier.weight(1f).fillMaxHeight()) {
            composable("home") { HomeScreen() }
            composable("briefing") { BriefingScreen() }
            composable("drive") { DriveScreen() }
            composable("report") { ReportScreen() }
        }
    }
}
