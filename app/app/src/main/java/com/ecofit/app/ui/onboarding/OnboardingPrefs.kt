package com.ecofit.app.ui.onboarding

import android.content.Context

private const val PREFS_NAME = "ecofit_prefs"
private const val KEY_ONBOARDING_SEEN = "onboarding_seen"

// 첫 실행 안내를 한 번 봤는지. 앱 데이터를 지우기 전까진 다시 안 뜬다
object OnboardingPrefs {
    fun hasSeen(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_SEEN, false)

    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_SEEN, true)
            .apply()
    }
}
