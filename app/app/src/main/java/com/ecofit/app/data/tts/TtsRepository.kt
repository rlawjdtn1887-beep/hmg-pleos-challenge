package com.ecofit.app.data.tts

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "ecofit_prefs"
private const val KEY_TTS_ENABLED = "tts_enabled"

/**
 * TextToSpeech SDK 연동. 담당: 정수
 *
 * ON_DEVICE 모드만 쓴다. SERVER/HYBRID는 registerApp()에 Pleos가 발급하는
 * 앱 등록 키가 필요한데 아직 없음. ON_DEVICE는 키 없이 기기 내 CAAS 엔진에
 * bind만 하면 돼서 지금 당장 시도 가능한 경로.
 *
 * initialize() 완료 전에 speak()가 오면 무시한다 (ready 플래그로 가드).
 *
 * 말하는 도중 새 문장이 오면 앞 문장을 끊지 않고 건너뛴다. 끊고 다시 시작하면
 * "이이잉 이십원"처럼 들린다. 꼭 끼어들어야 하는 문장만 interrupt = true.
 */
class TtsRepository(context: Context) {

    private val tag = "TtsRepository"
    private val tts = TextToSpeech(context.applicationContext, Mode.ON_DEVICE)
    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var speaking = false
    private val lastSpokenAt = HashMap<String, Long>()

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_TTS_ENABLED, true))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val listener = object : EventListener {
        override fun onReady() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onStart() { speaking = true }
        override fun onDone() = finished()
        override fun onStop() = finished()
        override fun onError(errorType: ErrorType) {
            Log.w(tag, "speak failed: $errorType")
            finished()
        }
    }

    // 끝났다는 신호가 안 올 때가 있어서 글자 수로 잡은 시간이 지나면 끝난 걸로 본다
    private val release = Runnable { speaking = false }

    private fun finished() {
        main.removeCallbacks(release)
        speaking = false
    }

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        prefs.edit().putBoolean(KEY_TTS_ENABLED, value).apply()
        if (!value) stopSpeaking()
    }

    fun start() {
        tts.initialize(object : OnInitializeListener {
            override fun onSuccess() {
                // setLocale 안 하면 en_US로 고정돼서 한국어 텍스트를 넣으면 조용히 무음 처리됨.
                // Locale.KOREAN("ko")만 주면 온디바이스 엔진이 "unsupported locale: ko-"로
                // 바로 실패한다. 국가 코드까지 있는 ko-KR을 써야 함
                tts.setLocale(Locale("ko", "KR"))
                // speaker를 안 정하면 CAAS가 내부 기본값 "lyra"를 먼저 시도했다가 온디바이스
                // 한국어에서 지원 안 돼서 매번 "HAEUN"으로 폴백한다. 이 폴백이 매 발화 생성 직전에
                // 에러 경로로 끼어들면서 시작 부분이 지지직거리는 원인으로 보여 아예 HAEUN으로 고정
                tts.setSpeakerType("HAEUN")
                tts.addEventListener(listener)
                ready = true
                Log.i(tag, "TTS ready (ON_DEVICE, ko)")
            }

            override fun onFailure(error: ErrorType) {
                ready = false
                Log.w(tag, "TTS init failed: $error")
            }
        })
    }

    /**
     * @param interrupt true면 말하던 걸 끊고 바로 말한다. 급제동 경고처럼 늦으면 의미 없는 것만.
     * @param key 같은 종류 문장을 묶는 이름. minGapMs 안에 같은 key가 또 오면 건너뛴다.
     */
    fun speak(text: String, interrupt: Boolean = false, key: String? = null, minGapMs: Long = 0) {
        if (!ready || !_enabled.value) return
        val now = System.currentTimeMillis()
        if (key != null && minGapMs > 0) {
            val last = lastSpokenAt[key] ?: Long.MIN_VALUE / 2
            if (now - last < minGapMs) return
        }
        if (speaking && !interrupt) return
        if (key != null) lastSpokenAt[key] = now
        speaking = true
        main.removeCallbacks(release)
        main.postDelayed(release, 1500L + text.length * 180L)
        try {
            tts.speak(text, true)
        } catch (e: RuntimeException) {
            Log.w(tag, "speak threw: $e")
            finished()
        }
    }

    fun stopSpeaking() {
        if (ready) runCatching { tts.stop() }
        finished()
    }

    fun stop() {
        ready = false
        if (speaking) runCatching { tts.stop() }
        runCatching { tts.removeEventListener(listener) }
        tts.release()
    }
}
