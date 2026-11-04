package com.ecofit.app.data.tts

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
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
 */
class TtsRepository(context: Context) {

    private val tag = "TtsRepository"
    private val tts = TextToSpeech(context.applicationContext, Mode.ON_DEVICE)
    private var ready = false

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_TTS_ENABLED, true))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        prefs.edit().putBoolean(KEY_TTS_ENABLED, value).apply()
        if (!value) tts.stop()
    }

    fun start() {
        tts.initialize(object : OnInitializeListener {
            override fun onSuccess() {
                // setLocale 안 하면 en_US로 고정돼서 한국어 텍스트를 넣으면 조용히 무음 처리됨.
                // Locale.KOREAN("ko")만 주면 온디바이스 엔진이 "unsupported locale: ko-"로
                // 바로 실패한다. 국가 코드까지 있는 ko-KR을 써야 함
                tts.setLocale(Locale("ko", "KR"))
                ready = true
                Log.i(tag, "TTS ready (ON_DEVICE, ko)")
            }

            override fun onFailure(error: ErrorType) {
                ready = false
                Log.w(tag, "TTS init failed: $error")
            }
        })
    }

    fun speak(text: String) {
        if (!ready || !_enabled.value) return
        tts.speak(text, true)
    }

    fun stop() {
        ready = false
        tts.release()
    }
}
