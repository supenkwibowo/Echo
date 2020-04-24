package com.cecekfosfore.app

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.cecekfosfore.app.PermissionManager.Permission.ALLOWED
import com.cecekfosfore.app.PermissionManager.Permission.NOT_ALLOWED
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), VoiceRecorder.DecibelMeterListener {

    private val permissionManager = PermissionManager(this)
    private val appConfig by lazy { AppConfig(PreferenceManager.getDefaultSharedPreferences(this)) }
    private val audioRecorder = VoiceRecorder().apply { decibelMeterListener = this@MainActivity }
    private val audioService by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private var isRecording = false
        set(value) {
            field = value
            if (value) {
                record_button.setImageResource(R.drawable.ic_record_on)
            } else {
                record_button.setImageResource(R.drawable.ic_record_off)
                decibleMeter.apply {
                    progress = 0
                    secondaryProgress = 0
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        gain_control.updateGainLabelOnChange()
        gain_control.progress = (appConfig.gain * 2).toInt()
        decibleMeter.max = 500

        record_button.setOnClickListener {
            if (!isRecording) {
                permissionManager.request {
                    when (it) {
                        ALLOWED -> startRecording()
                        NOT_ALLOWED -> Toast.makeText(
                            this,
                            "Need record permission",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                stopRecording()
            }
        }

        setting_button.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun startRecording() {
        isRecording = true
        audioService.mode = AudioManager.MODE_IN_COMMUNICATION
        val channel = when (appConfig.channel) {
            AppConfig.Channel.MONO -> RecordingChannel.MONO
            AppConfig.Channel.STEREO -> RecordingChannel.STEREO
        }
        audioRecorder.startRecord(RecordingConfig(channel, appConfig.sampleRate))
    }

    private fun SeekBar.updateGainLabelOnChange() {
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, isFromUser: Boolean) {
                updateGainLabel()
                gain.apply {
                    audioRecorder.gain = this
                    appConfig.gain = this
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        updateGainLabel()
    }

    private fun SeekBar.updateGainLabel() {
        gain_label.text = "Gain: ${gain}"
    }

    private val SeekBar.gain get() = progress / 2f

    private fun stopRecording() {
        isRecording = false
        audioRecorder.stopRecord()
        audioService.mode = AudioManager.MODE_NORMAL
    }

    override fun onStop() {
        super.onStop()
        stopRecording()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) = permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)

    override fun onUpdate(decible: Int) {
        decibleMeter.progress = decible
        decibleMeter.apply {
            progress = decible
            secondaryProgress = decible.coerceAtLeast(secondaryProgress)
        }
    }

}
