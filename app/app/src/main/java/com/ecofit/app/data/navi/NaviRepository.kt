package com.ecofit.app.data.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * NaviHelper SDK 연동. 담당: 정수
 *
 * BriefingScreen(출발 전)의 "목적지까지 남은 거리"를 채우는 용도.
 * 사용자가 내비 앱에 목적지를 설정해둔 경우에만 값이 들어오고,
 * 없으면 remainDistanceKm은 null로 유지된다 (하드코딩 42f 제거).
 *
 * Vehicle SDK와 달리 NaviHelper는 실제 내비 앱(ai.umos.maps...)이 응답해야
 * 콜백이 오므로, HVAC처럼 "등록은 되는데 콜백이 안 오는" 상황이 재발할 수 있다.
 * 그럴 땐 getDestinationInfo() 호출 직후 Logcat에서 onDestinationInfo/onError
 * 수신 여부부터 확인할 것.
 *
 * getDestinationInfo()는 1회성 요청이라, 주행 중 위치가 바뀌어도 자동으로는
 * 안 갱신된다. POLL_INTERVAL_MS 주기로 다시 요청해서 값을 최신으로 유지한다.
 */
class NaviRepository(context: Context) {

    private val tag = "NaviRepository"
    private val naviHelper = NaviHelper(context.applicationContext)
    private var initialized = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null

    // 실제 내비에서 받은 값
    private val _remainDistanceKm = MutableStateFlow<Float?>(null)
    private val _remainTimeMin = MutableStateFlow<Int?>(null)

    // 데모 주행 중에만 쓰는 가짜 목적지. 데모가 끝나면 null로 돌아가 실제 값이 다시 보인다
    private val demoDistanceKm = MutableStateFlow<Float?>(null)
    private val demoTimeMin = MutableStateFlow<Int?>(null)
    private val demoName = MutableStateFlow<String?>(null)

    val remainDistanceKm: StateFlow<Float?> = combine(_remainDistanceKm, demoDistanceKm) { real, demo -> demo ?: real }
        .stateIn(scope, SharingStarted.Eagerly, null)
    val remainTimeMin: StateFlow<Int?> = combine(_remainTimeMin, demoTimeMin) { real, demo -> demo ?: real }
        .stateIn(scope, SharingStarted.Eagerly, null)
    // NaviHelper는 목적지 이름을 주지 않아서 실제 주행에선 null, 데모에서만 채워진다
    val destinationName: StateFlow<String?> = demoName.asStateFlow()

    private val listener = object : NaviHelperEventListener {
        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            _remainDistanceKm.value = destinationInfo.remainDistance / 1000f
            _remainTimeMin.value = destinationInfo.remainTime / 60
        }

        override fun onDestinationChanged() {
            // 목적지가 바뀌거나 삭제됐으니 다시 조회
            naviHelper.getDestinationInfo()
        }

        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
            _remainDistanceKm.value = null
            _remainTimeMin.value = null
        }

        override fun onError(errorCode: NaviErrorCode) {
            Log.w(tag, "NaviHelper error: $errorCode")
        }
    }

    fun start() {
        if (initialized) return
        naviHelper.addListener(listener)
        naviHelper.initialize(
            onConnected = {
                initialized = true
                naviHelper.getDestinationInfo()
                startPolling()
            },
            onFailed = { e -> Log.e(tag, "NaviHelper connect failed", e) },
        )
    }

    // 브리핑 화면의 "충전소를 넣어주세요" 제안을 탭했을 때 실행.
    // POI 좌표를 직접 찾아서 addWaypoint()로 넣는 API는 placesense 같은 장소 검색 SDK가
    // 따로 있어야 해서(없음), 대신 내비 앱 자체의 "주변 충전소" 화면을 띄워 거기서 고르게 한다
    fun showNearbyChargingStations() {
        naviHelper.requestNavigationScreen(NavigationScreenRequest.NearbyChargingStations)
    }

    // 데모 주행 시작. 목적지를 하나 잡아 두고, 데모가 달린 거리만큼 남은 거리를 줄인다
    fun startDemo() {
        demoName.value = DEMO_DEST_NAME
        updateDemo(0f)
    }

    fun updateDemo(traveledKm: Float) {
        val remain = (DEMO_DEST_KM - traveledKm).coerceAtLeast(0.1f)
        demoDistanceKm.value = remain
        demoTimeMin.value = (remain / DEMO_AVG_KMH * 60f).roundToInt().coerceAtLeast(1)
    }

    fun stopDemo() {
        demoName.value = null
        demoDistanceKm.value = null
        demoTimeMin.value = null
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                naviHelper.getDestinationInfo()
            }
        }
    }

    fun stop() {
        if (!initialized) return
        pollJob?.cancel()
        pollJob = null
        naviHelper.removeListener(listener)
        naviHelper.release()
        initialized = false
    }

    companion object {
        private const val POLL_INTERVAL_MS = 15_000L
        private const val DEMO_DEST_NAME = "현대자동차 본사"
        private const val DEMO_DEST_KM = 12f
        private const val DEMO_AVG_KMH = 40f
    }
}
