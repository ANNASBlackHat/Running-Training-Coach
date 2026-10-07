package com.runningcompanion.app.service.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.runningcompanion.app.domain.engine.CueEvent
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentType
import java.util.Locale
import java.util.UUID

class AudioCueEngine(
    private val context: Context,
    private val onInitComplete: (() -> Unit)? = null
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var toneGenerator: ToneGenerator? = null
    private var currentFocusRequest: AudioFocusRequest? = null

    var isVoiceEnabled: Boolean = true
    var isBeepsEnabled: Boolean = true

    init {
        tts = TextToSpeech(context.applicationContext, this)
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        } catch (_: Exception) {
            // ToneGenerator might fail on some minimal emulators without audio hardware
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    releaseAudioFocus()
                }
                override fun onError(utteranceId: String?) {
                    releaseAudioFocus()
                }
            })
            isTtsReady = true
            onInitComplete?.invoke()
        }
    }

    fun playCue(cue: CueEvent) {
        when (cue) {
            is CueEvent.Countdown -> {
                if (isBeepsEnabled) {
                    playCountdownBeep(cue.secondsRemaining)
                }
            }
            is CueEvent.SegmentStart -> {
                if (isVoiceEnabled) {
                    val text = buildSegmentStartSpeech(cue)
                    speak(text, isHighPriority = true)
                }
            }
            is CueEvent.Halfway -> {
                if (isVoiceEnabled) speak("Halfway", isHighPriority = false)
            }
            is CueEvent.TimeRemaining -> {
                if (isVoiceEnabled) speak("${cue.secondsLeft / 60} minute left", isHighPriority = false)
            }
            is CueEvent.DistanceRemaining -> {
                if (isVoiceEnabled) speak("${cue.metersLeft} meters left", isHighPriority = false)
            }
            is CueEvent.PaceAlert -> {
                if (isVoiceEnabled) {
                    val alertDirection = if (cue.direction == CueEvent.PaceAlertDirection.TOO_FAST) "Too fast" else "Too slow"
                    val currentPaceStr = com.runningcompanion.app.domain.math.PaceCalculator.formatPaceSpeech(cue.currentPaceSecPerKm)
                    val fastStr = com.runningcompanion.app.domain.math.PaceCalculator.formatPaceSpeech(cue.paceRange.fastSecPerKm.toDouble())
                    val slowStr = com.runningcompanion.app.domain.math.PaceCalculator.formatPaceSpeech(cue.paceRange.slowSecPerKm.toDouble())
                    val text = "$alertDirection: $currentPaceStr. Target $fastStr to $slowStr."
                    speak(text, isHighPriority = false)
                }
            }
            is CueEvent.BackOnPace -> {
                if (isVoiceEnabled) {
                    val currentPaceStr = com.runningcompanion.app.domain.math.PaceCalculator.formatPaceSpeech(cue.currentPaceSecPerKm)
                    val text = "Back on target pace: $currentPaceStr."
                    speak(text, isHighPriority = false)
                }
            }
            is CueEvent.KmSplitAlert -> {
                if (isVoiceEnabled) {
                    val kmWord = if (cue.kilometer == 1) "1 kilometer" else "${cue.kilometer} kilometers"
                    val splitPaceStr = com.runningcompanion.app.domain.math.PaceCalculator.formatPaceSpeech(cue.splitPaceSecPerKm)
                    val totalTimeStr = com.runningcompanion.app.domain.math.PaceCalculator.formatTimeSpeech(cue.totalDurationSec)
                    val text = "$kmWord. Split pace $splitPaceStr. Total time $totalTimeStr."
                    speak(text, isHighPriority = false)
                }
            }
            is CueEvent.SetsLeft -> {
                if (isVoiceEnabled) {
                    val word = if (cue.setsRemaining == 1) "set" else "sets"
                    speak("${cue.setsRemaining} $word left", isHighPriority = false)
                }
            }
            is CueEvent.WorkoutComplete -> {
                if (isVoiceEnabled) speak("Workout complete", isHighPriority = true)
            }
        }
    }

    private fun buildSegmentStartSpeech(cue: CueEvent.SegmentStart): String {
        val typeWord = when (cue.type) {
            SegmentType.RUN -> "Run"
            SegmentType.REST -> "Rest"
            SegmentType.WARMUP -> "Warm up"
            SegmentType.COOLDOWN -> "Cool down"
        }

        val lengthWord = when (val len = cue.length) {
            is SegmentLength.Time -> {
                val mins = len.seconds / 60
                val secs = len.seconds % 60
                if (secs == 0) "$mins minutes" else "$mins minutes $secs seconds"
            }
            is SegmentLength.Distance -> "${len.meters} meters"
        }

        val setAnnouncement = if (cue.setNumber != null && cue.setTotal != null && cue.type == SegmentType.RUN) {
            "Set ${cue.setNumber} of ${cue.setTotal}. "
        } else ""

        return "$setAnnouncement$typeWord. $lengthWord."
    }

    private fun playCountdownBeep(secondsRemaining: Int) {
        try {
            if (secondsRemaining == 0) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 500)
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            }
        } catch (_: Exception) {}
    }

    private fun speak(text: String, isHighPriority: Boolean) {
        if (!isTtsReady) return
        val activeTts = tts ?: return

        if (!isHighPriority && activeTts.isSpeaking) {
            // Drop low priority cues if audio is already speaking
            return
        }

        requestAudioFocus()
        val queueMode = if (isHighPriority) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = UUID.randomUUID().toString()
        activeTts.speak(text, queueMode, null, utteranceId)
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .build()
            currentFocusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                null,
                AudioManager.STREAM_NOTIFICATION,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun releaseAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            currentFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            toneGenerator?.release()
        } catch (_: Exception) {}
        releaseAudioFocus()
    }
}
