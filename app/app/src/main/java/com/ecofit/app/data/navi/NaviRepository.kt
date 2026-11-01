package com.ecofit.app.data.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val _remainDistanceKm = MutableStateFlow<Float?>(null)
    val remainDistanceKm: StateFlow<Float?> = _remainDistanceKm.asStateFlow()

    private val _remainTimeMin = MutableStateFlow<Int?>(null)
    val remainTimeMin: StateFlow<Int?> = _remainTimeMin.asStateFlow()

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
    }
}
