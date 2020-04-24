package com.cecekfosfore.app

import android.media.*
import android.os.Looper
import android.os.Process
import android.util.Log
import kotlin.math.abs

class VoiceRecorder {

    interface DecibelMeterListener {
        fun onUpdate(decibel: Int)
    }

    var decibelMeterListener: DecibelMeterListener? = null
    var gain: Float = 1f

    private var recordingThread: RecordingThread? = null
    private var isRecording = false

    fun startRecord(config: RecordingConfig) {
        ensureMainThread()
        if (isRecording) {
            return
        }
        isRecording = true
        recordingThread = RecordingThread(Runnable {
            val recorder = AudioRecordFactory.create(config)
            val player = AudioTrackFactory.create(config)
            recorder.startRecording()
            player.play()
            while (isRecording) {
                var buffer = ShortArray(AudioRecordFactory.minimumBufferSizeInBytes(config) / 2)
                val readCount = recorder.read(buffer, 0, buffer.size)
                decibelMeterListener?.notifyUpdate(buffer)
                player.write(buffer.adjustGainBy(gain), 0, readCount)
            }
            player.stop()
            recorder.stop()
        }).apply { start() }
    }

    private fun ShortArray.adjustGainBy(multiplier: Float) =
        map(Short::toInt)
            .map { it * multiplier }
            .map { it.coerceAtMost(Short.MAX_VALUE.toFloat()) }
            .map(Float::toShort).toShortArray()

    private fun DecibelMeterListener.notifyUpdate(buffer: ShortArray) {
        val decibel = buffer.map(Short::toInt).map(::abs).sum() / buffer.size
        onUpdate(decibel)
    }

    fun stopRecord() {
        ensureMainThread()
        isRecording = false
    }

    private fun ensureMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw NotOnMainThreadException
    }
}

object NotOnMainThreadException : Exception()

enum class RecordingChannel {
    MONO, STEREO;

    val channelIn: Int
        get() = when (this) {
            MONO -> AudioFormat.CHANNEL_IN_MONO
            STEREO -> AudioFormat.CHANNEL_IN_STEREO
        }

    val channelOut: Int
        get() = when (this) {
            MONO -> AudioFormat.CHANNEL_OUT_MONO
            STEREO -> AudioFormat.CHANNEL_OUT_STEREO
        }
}

data class RecordingConfig(
    val channel: RecordingChannel,
    val sampleRateInHz: Int
) {
    val recordChannel = channel.channelIn
    val playbackChannel = channel.channelOut

    val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
}

//    val sampleRates = arrayOf(8000, 11025, 22050, 44100)
private object AudioRecordFactory {
    fun minimumBufferSizeInBytes(config: RecordingConfig): Int {
        return AudioRecord.getMinBufferSize(
            config.sampleRateInHz,
            config.recordChannel,
            config.audioEncoding
        )
    }

    fun create(config: RecordingConfig): AudioRecord {
        val minimumBufferSize = minimumBufferSizeInBytes(config)
        return AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(config.sampleRateInHz)
                    .setEncoding(config.audioEncoding)
                    .setChannelMask(config.recordChannel)
                    .build()
            )
            .setBufferSizeInBytes(minimumBufferSize)
            .build()
    }
}

private object AudioTrackFactory {
    fun create(config: RecordingConfig): AudioTrack {
        val maxJitter = AudioTrack.getMinBufferSize(
            config.sampleRateInHz,
            config.playbackChannel,
            config.audioEncoding
        )
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setBufferSizeInBytes(maxJitter)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(config.audioEncoding)
                    .setSampleRate(config.sampleRateInHz)
                    .setChannelMask(config.playbackChannel)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setSessionId(AudioManager.AUDIO_SESSION_ID_GENERATE)
            .build()
    }
}

private class RecordingThread(runnable: Runnable) : Thread(runnable) {
    override fun start() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        super.start()
    }
}