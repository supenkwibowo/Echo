package com.cecekfosfore.app

import android.content.SharedPreferences
import androidx.core.content.edit

class AppConfig(private val preference: SharedPreferences) {
    enum class Channel {
        MONO, STEREO
    }

    val channel: Channel
        get() = when (preference.getString("recording_channel", "Mono")) {
            "Mono" -> Channel.MONO
            "Stereo" -> Channel.STEREO
            else -> Channel.MONO
        }
    val sampleRate: Int get() = preference.getString("sample_rate", "8000")!!.toInt()
    var gain: Float
        get() = preference.getFloat("gain", 1f)
        set(value) {
            preference.edit { putFloat("gain", value) }
        }
}
