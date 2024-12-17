package com.hiennv.flutter_callkit_incoming

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.text.TextUtils

class CallkitSoundPlayerService : Service() {

    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null

    private var mediaPlayer: MediaPlayer? = null
    private var data: Bundle? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        this.prepare()
        this.playSound(intent)
        this.playVibrator()
        return START_STICKY;
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        vibrator?.cancel()

        mediaPlayer = null
        vibrator = null
    }

    private fun prepare() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        vibrator?.cancel()
    }

    private fun playVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                this.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        audioManager = this.getSystemService(AUDIO_SERVICE) as AudioManager
        when (audioManager?.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> {
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0L, 1000L, 1000L),
                            0
                        )
                    )
                } else {
                    vibrator?.vibrate(longArrayOf(0L, 1000L, 1000L), 0)
                }
            }
        }
    }

    private fun playSound(intent: Intent?) {
        this.data = intent?.extras
        val sound = this.data?.getString(
            CallkitConstants.EXTRA_CALLKIT_RINGTONE_PATH,
            ""
        )
        var uri = sound?.let { getRingtoneUri(it) }
        if (uri == null) {
            uri = RingtoneManager.getActualDefaultRingtoneUri(
                this@CallkitSoundPlayerService,
                RingtoneManager.TYPE_RINGTONE
            )
        }
        try {
            mediaPlayer(uri!!)
        } catch (e: Exception) {
            try {
                uri = getRingtoneUri("ringtone_default")
                mediaPlayer(uri!!)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    private fun mediaPlayer(uri: Uri) {
        mediaPlayer = MediaPlayer()
        mediaPlayer.reset()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val attribution = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setLegacyStreamType(AudioManager.STREAM_RING)
                .build()
            mediaPlayer?.setAudioAttributes(attribution)
        } else {
            mediaPlayer?.setAudioStreamType(AudioManager.STREAM_RING)
        }
        setDataSource(uri)
        mediaPlayer?.prepare()
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    private fun setDataSource(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val assetFileDescriptor =
                applicationContext.contentResolver.openAssetFileDescriptor(uri, "r")
            if (assetFileDescriptor != null) {
                mediaPlayer?.setDataSource(assetFileDescriptor)
            }
            return
        }
        mediaPlayer?.setDataSource(applicationContext, uri)
    }

   private fun getRingtoneUri(fileName: String): Uri? {
    return try {
        // Check if fileName is empty, and if so, return the default system ringtone
        if (TextUtils.isEmpty(fileName)) {
            return RingtoneManager.getActualDefaultRingtoneUri(
                this@CallkitSoundPlayerService,
                RingtoneManager.TYPE_RINGTONE
            )
        }
        
        // Try to retrieve the resource identifier for the ringtone file
        val resId = resources.getIdentifier(fileName, "raw", packageName)
        
        if (resId != 0) {
            // If the resource is found, return its URI
            return Uri.parse("android.resource://${packageName}/$resId")
        } else {
            val resId = resources.getIdentifier(fileName, "raw", packageName)
            if (resId != 0) {
                // If the resource is found, return its URI
                return Uri.parse("android.resource://${packageName}/$resId")
            }
            // If the resource wasn't found, check if it matches the known system default ringtone name
            if (fileName.equals("system_ringtone_default", true)) {
                return RingtoneManager.getActualDefaultRingtoneUri(
                    this@CallkitSoundPlayerService,
                    RingtoneManager.TYPE_RINGTONE
                )
            } else {
                // Fallback to the default system ringtone
                return RingtoneManager.getActualDefaultRingtoneUri(
                    this@CallkitSoundPlayerService,
                    RingtoneManager.TYPE_RINGTONE
                )
            }
        }
    } catch (e: Exception) {
        // In case of error, fall back to the default system ringtone
            e.printStackTrace()
            return RingtoneManager.getActualDefaultRingtoneUri(
                this@CallkitSoundPlayerService,
                RingtoneManager.TYPE_RINGTONE
            )
        }
    }

}