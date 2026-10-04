package com.runningcompanion.app.service.foreground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.runningcompanion.app.MainActivity
import com.runningcompanion.app.data.location.AndroidLocationManager
import com.runningcompanion.app.data.location.LocationProvider
import com.runningcompanion.app.domain.engine.RunnerSnapshot
import com.runningcompanion.app.domain.engine.RunnerState
import com.runningcompanion.app.domain.engine.WorkoutRunner
import com.runningcompanion.app.domain.math.PaceCalculator
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.domain.model.Workout
import com.runningcompanion.app.service.audio.AudioCueEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class WorkoutForegroundService : Service() {

    inner class LocalBinder : Binder() {
        val service: WorkoutForegroundService get() = this@WorkoutForegroundService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null

    private var runner: WorkoutRunner? = null
    private var audioCueEngine: AudioCueEngine? = null
    private var locationProvider: LocationProvider? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val _snapshot = MutableStateFlow<RunnerSnapshot?>(null)
    val snapshot: StateFlow<RunnerSnapshot?> = _snapshot.asStateFlow()

    private val _sessionCompleted = MutableSharedFlow<Session>(extraBufferCapacity = 1)
    val sessionCompleted: SharedFlow<Session> = _sessionCompleted.asSharedFlow()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioCueEngine = AudioCueEngine(applicationContext)
        locationProvider = AndroidLocationManager(applicationContext)

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "RunningCompanion:WorkoutWakeLock"
        )
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val workoutJson = intent.getStringExtra(EXTRA_WORKOUT_JSON)
                if (workoutJson != null) {
                    val workout = Json.decodeFromString(Workout.serializer(), workoutJson)
                    startWorkout(workout)
                }
            }
            ACTION_PAUSE -> pauseWorkout()
            ACTION_RESUME -> resumeWorkout()
            ACTION_SKIP -> skipSegment()
            ACTION_STOP -> stopWorkout()
        }
        return START_NOT_STICKY
    }

    fun startWorkout(workout: Workout) {
        val activeRunner = WorkoutRunner(workout)
        runner = activeRunner

        wakeLock?.acquire(3 * 60 * 60 * 1000L) // 3 hours max safe timeout

        // Start Foreground Service with Notification
        startForegroundServiceNotification(activeRunner)

        val nowMs = System.currentTimeMillis()
        val startCues = activeRunner.start(nowMs)
        startCues.forEach { audioCueEngine?.playCue(it) }

        // Start GPS updates
        locationProvider?.start { point ->
            val curRunner = runner ?: return@start
            val now = System.currentTimeMillis()
            val cues = curRunner.onLocation(point, now)
            cues.forEach { audioCueEngine?.playCue(it) }
            updateSnapshotAndNotification(now)
            checkIfFinished(curRunner, now)
        }

        // Start Ticker (1000ms loop)
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                val curRunner = runner ?: break
                val now = System.currentTimeMillis()
                val cues = curRunner.tick(now)
                cues.forEach { audioCueEngine?.playCue(it) }
                updateSnapshotAndNotification(now)
                checkIfFinished(curRunner, now)
            }
        }
    }

    fun pauseWorkout() {
        val now = System.currentTimeMillis()
        runner?.pause(now)
        updateSnapshotAndNotification(now)
    }

    fun resumeWorkout() {
        val now = System.currentTimeMillis()
        runner?.resume(now)
        updateSnapshotAndNotification(now)
    }

    fun skipSegment() {
        val now = System.currentTimeMillis()
        val cues = runner?.skip(now) ?: emptyList()
        cues.forEach { audioCueEngine?.playCue(it) }
        updateSnapshotAndNotification(now)
        runner?.let { checkIfFinished(it, now) }
    }

    fun stopWorkout() {
        val now = System.currentTimeMillis()
        val finalSession = runner?.stop(now)
        cleanUp()
        if (finalSession != null) {
            _sessionCompleted.tryEmit(finalSession)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun checkIfFinished(curRunner: WorkoutRunner, now: Long) {
        if (curRunner.activeState == RunnerState.FINISHED) {
            val session = curRunner.buildSessionResult(now)
            cleanUp()
            _sessionCompleted.tryEmit(session)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun updateSnapshotAndNotification(nowMs: Long) {
        val curRunner = runner ?: return
        val snap = curRunner.getSnapshot(nowMs)
        _snapshot.value = snap
        updateNotification(snap)
    }

    private fun cleanUp() {
        tickerJob?.cancel()
        tickerJob = null
        locationProvider?.stop()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun startForegroundServiceNotification(activeRunner: WorkoutRunner) {
        val initialSnap = activeRunner.getSnapshot(System.currentTimeMillis())
        val notification = buildNotification(initialSnap)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(snap: RunnerSnapshot) {
        val notification = buildNotification(snap)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(snap: RunnerSnapshot): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val seg = snap.currentSegment
        val typeName = when (seg?.segment?.type) {
            SegmentType.RUN -> "Run"
            SegmentType.REST -> "Rest"
            SegmentType.WARMUP -> "Warm-up"
            SegmentType.COOLDOWN -> "Cool-down"
            null -> "Workout"
        }

        val remainingText = if (snap.segmentRemainingSec != null) {
            PaceCalculator.formatTime(snap.segmentRemainingSec)
        } else if (snap.segmentRemainingM != null) {
            "${snap.segmentRemainingM.toInt()}m"
        } else ""

        val paceText = PaceCalculator.formatPace(snap.currentPaceSecPerKm)
        val title = "$typeName | $remainingText left"
        val content = "Pace: $paceText/km • Total: %.2f km".format(snap.totalDistanceM / 1000.0)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(snap.state == RunnerState.RUNNING || snap.state == RunnerState.PAUSED)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Running Session Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live running segment and pace"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        cleanUp()
        audioCueEngine?.shutdown()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "running_workout_channel"

        const val ACTION_START = "com.runningcompanion.app.action.START"
        const val ACTION_PAUSE = "com.runningcompanion.app.action.PAUSE"
        const val ACTION_RESUME = "com.runningcompanion.app.action.RESUME"
        const val ACTION_SKIP = "com.runningcompanion.app.action.SKIP"
        const val ACTION_STOP = "com.runningcompanion.app.action.STOP"

        const val EXTRA_WORKOUT_JSON = "extra_workout_json"

        fun startIntent(context: Context, workout: Workout): Intent {
            return Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_WORKOUT_JSON, Json.encodeToString(Workout.serializer(), workout))
            }
        }
    }
}
