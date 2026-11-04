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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ecofit.app.data.model.Gear
import com.ecofit.app.ui.EcoLogo
import com.ecofit.app.ui.briefing.BriefingScreen
import com.ecofit.app.ui.drive.DriveScreen
import com.ecofit.app.ui.home.HomeScreen
import com.ecofit.app.ui.onboarding.OnboardingPrefs
import com.ecofit.app.ui.onboarding.OnboardingScreen
import com.ecofit.app.ui.report.ReportScreen
import com.ecofit.app.ui.theme.Black
import com.ecofit.app.ui.theme.EcoFitTheme
import com.ecofit.app.ui.theme.Ink
import com.ecofit.app.ui.theme.InkMuted
import com.ecofit.app.ui.theme.Paper
import com.ecofit.app.ui.tour.Tour
import com.ecofit.app.ui.tour.TourOverlay

class MainActivity : ComponentActivity() {

    // CAR_SPEED는 런타임 권한. 허용되면 차속 구독을 다시 시도한다
    private val speedPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) EcoFitApp.instance.vehicleRepo.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EcoFitTheme { EcoFitNav(onRequestSpeedPermission = ::requestSpeedPermission) } }
    }

    // 권한 창은 주행 중 제한에 걸리므로 P에서만 띄운다. 이미 허용됐으면 아무것도 안 한다
    private fun requestSpeedPermission(): Boolean {
        if (checkSelfPermission(CAR_SPEED) == PackageManager.PERMISSION_GRANTED) return false
        speedPermission.launch(CAR_SPEED)
        return true
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
fun EcoFitNav(onRequestSpeedPermission: () -> Boolean = { false }) {
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route
    val snap by EcoFitApp.instance.vehicleRepo.snapshot.collectAsStateWithLifecycle()
    var asked by remember { mutableStateOf(false) }
    var prevGear by remember { mutableStateOf<Gear?>(null) }
    var showOnboarding by remember { mutableStateOf(!OnboardingPrefs.hasSeen(EcoFitApp.instance)) }
    val context = LocalContext.current

    // 안내 투어 단계마다 해당 화면으로 옮겨 준다
    LaunchedEffect(Tour.step) {
        val route = Tour.current?.route ?: return@LaunchedEffect
        if (current != route) nav.navigate(route) {
            launchSingleTop = true
            popUpTo("home")
        }
    }

    // P에 있을 때 한 번만 권한을 묻는다
    LaunchedEffect(snap.gear) {
        if (!asked && snap.gear == Gear.P) {
            asked = true
            onRequestSpeedPermission()
        }
    }

    // 기어가 바뀌면 화면도 따라간다. 출발하면 주행 중, 세우면 리포트
    LaunchedEffect(snap.gear) {
        val g = snap.gear
        val p = prevGear
        prevGear = g
        if (p == null) return@LaunchedEffect
        val driving = g == Gear.D || g == Gear.R
        val wasDriving = p == Gear.D || p == Gear.R
        val target = when {
            driving && !wasDriving -> "drive"
            !driving && wasDriving && g == Gear.P -> "report"
            else -> null
        }
        if (target == "drive") EcoFitApp.instance.ttsRepo.speak("주행을 시작해요")
        if (target != null && current != target) {
            nav.navigate(target) {
                launchSingleTop = true
                popUpTo("home")
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
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

                Spacer(Modifier.weight(1f))

                val ttsEnabled by EcoFitApp.instance.ttsRepo.enabled.collectAsStateWithLifecycle()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "음성 안내", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = InkMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = ttsEnabled,
                        onCheckedChange = { EcoFitApp.instance.ttsRepo.setEnabled(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Ink),
                    )
                }
            }

            NavHost(nav, startDestination = "home", modifier = Modifier.weight(1f).fillMaxHeight()) {
                composable("home") { HomeScreen() }
                composable("briefing") { BriefingScreen() }
                composable("drive") { DriveScreen() }
                composable("report") { ReportScreen() }
            }
        }

        if (Tour.active) {
            TourOverlay(onFinish = { Tour.finish(context) }, onNext = { Tour.next(context) })
        }

        if (showOnboarding) {
            OnboardingScreen(
                onStart = {
                    OnboardingPrefs.markSeen(EcoFitApp.instance)
                    showOnboarding = false
                    // 소개를 보고 시작하면 화면을 하나씩 짚어 주는 투어로 이어진다
                    Tour.startIfFirstRun(context)
                },
                onDemo = {
                    OnboardingPrefs.markSeen(EcoFitApp.instance)
                    showOnboarding = false
                    EcoFitApp.instance.ttsRepo.speak("데모 주행을 시작합니다")
                    EcoFitApp.instance.vehicleRepo.startDemo()
                },
            )
        }
    }
}
