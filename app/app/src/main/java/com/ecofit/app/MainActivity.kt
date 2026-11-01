package com.ecofit.app

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ecofit.app.ui.briefing.BriefingScreen
import com.ecofit.app.ui.drive.DriveScreen
import com.ecofit.app.ui.home.HomeScreen
import com.ecofit.app.ui.report.ReportScreen
import com.ecofit.app.ui.theme.EcoFitTheme

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

private data class Dest(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val dests = listOf(
    Dest("home", "홈", Icons.Default.Home),
    Dest("briefing", "출발 전", Icons.Default.Info),
    Dest("drive", "주행 중", Icons.Default.PlayArrow),
    Dest("report", "리포트", Icons.Default.List),
)

@Composable
fun EcoFitNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row {
            NavigationRail(Modifier.fillMaxHeight().width(96.dp)) {
                dests.forEach { d ->
                    NavigationRailItem(
                        selected = current == d.route,
                        onClick = { if (current != d.route) nav.navigate(d.route) { launchSingleTop = true } },
                        icon = { Icon(d.icon, contentDescription = d.label) },
                        label = { Text(d.label) },
                    )
                }
            }
            NavHost(nav, startDestination = "home", modifier = Modifier.padding(24.dp)) {
                composable("home") { HomeScreen() }
                composable("briefing") { BriefingScreen() }
                composable("drive") { DriveScreen() }
                composable("report") { ReportScreen() }
            }
        }
    }
}
