package com.runningcompanion.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.runningcompanion.app.domain.engine.RunnerSnapshot
import com.runningcompanion.app.domain.model.RuntimeSegment
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.domain.model.Workout
import com.runningcompanion.app.domain.model.WorkoutTemplates
import com.runningcompanion.app.service.foreground.WorkoutForegroundService
import com.runningcompanion.app.ui.screens.builder.WorkoutBuilderScreen
import com.runningcompanion.app.ui.screens.detail.WorkoutDetailScreen
import com.runningcompanion.app.ui.screens.history.HistoryScreen
import com.runningcompanion.app.ui.screens.home.HomeScreen
import com.runningcompanion.app.ui.screens.results.ResultsScreen
import com.runningcompanion.app.ui.screens.run.LiveRunScreen
import com.runningcompanion.app.ui.theme.RunningTheme
import kotlinx.coroutines.launch

sealed interface Screen {
    object Home : Screen
    data class Detail(val workout: Workout) : Screen
    data class Builder(val initialWorkout: Workout? = null) : Screen
    data class Run(val workout: Workout) : Screen
    data class Results(val session: Session) : Screen
    object History : Screen
}

class MainActivity : ComponentActivity() {

    private var workoutService: WorkoutForegroundService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? WorkoutForegroundService.LocalBinder
            workoutService = localBinder?.service
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            workoutService = null
            isServiceBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, WorkoutForegroundService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as RunningApp).appContainer

        setContent {
            RunningTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
                    val coroutineScope = rememberCoroutineScope()

                    // Permissions launchers
                    var showBackgroundLocationPrompt by remember { mutableStateOf(false) }

                    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ -> }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { result ->
                        val fineGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
                        if (fineGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val bgMissing = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                            if (bgMissing) {
                                showBackgroundLocationPrompt = true
                            }
                        }
                    }

                    if (showBackgroundLocationPrompt && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showBackgroundLocationPrompt = false },
                            title = {
                                androidx.compose.material3.Text(
                                    text = "Enable Background Location",
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            },
                            text = {
                                androidx.compose.material3.Text(
                                    text = "To track workouts and receive audio guidance with your phone locked in your pocket, select 'Allow all the time' on the next screen."
                                )
                            },
                            confirmButton = {
                                androidx.compose.material3.Button(
                                    onClick = {
                                        showBackgroundLocationPrompt = false
                                        backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                    }
                                ) {
                                    androidx.compose.material3.Text("Continue")
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(
                                    onClick = { showBackgroundLocationPrompt = false }
                                ) {
                                    androidx.compose.material3.Text("Not now")
                                }
                            }
                        )
                    }

                    LaunchedEffect(Unit) {
                        val permissions = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        val missing = permissions.filter {
                            ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                        }
                        if (missing.isNotEmpty()) {
                            permissionLauncher.launch(missing.toTypedArray())
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val bgMissing = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                            if (bgMissing) {
                                showBackgroundLocationPrompt = true
                            }
                        }
                    }

                    // Workouts & History Flow
                    val allWorkouts by appContainer.workoutRepository.getAllWorkouts()
                        .collectAsState(initial = WorkoutTemplates.allTemplates)
                    val allSessions by appContainer.sessionRepository.getAllSessions()
                        .collectAsState(initial = emptyList())

                    // Listen to completed sessions from service
                    LaunchedEffect(workoutService) {
                        val service = workoutService ?: return@LaunchedEffect
                        service.sessionCompleted.collect { completedSession ->
                            appContainer.sessionRepository.saveSession(completedSession)
                            currentScreen = Screen.Results(completedSession)
                        }
                    }

                    when (val screen = currentScreen) {
                        is Screen.Home -> {
                            HomeScreen(
                                workouts = allWorkouts.ifEmpty { WorkoutTemplates.allTemplates },
                                onStartWorkout = { workout ->
                                    startWorkoutSession(workout)
                                    currentScreen = Screen.Run(workout)
                                },
                                onWorkoutDetail = { workout ->
                                    currentScreen = Screen.Detail(workout)
                                },
                                onNewWorkout = {
                                    currentScreen = Screen.Builder()
                                },
                                onHistoryClicked = {
                                    currentScreen = Screen.History
                                }
                            )
                        }

                        is Screen.Detail -> {
                            WorkoutDetailScreen(
                                workout = screen.workout,
                                onStartWorkout = {
                                    startWorkoutSession(screen.workout)
                                    currentScreen = Screen.Run(screen.workout)
                                },
                                onEditWorkout = {
                                    currentScreen = Screen.Builder(initialWorkout = screen.workout)
                                },
                                onBackClicked = {
                                    currentScreen = Screen.Home
                                }
                            )
                        }

                        is Screen.Builder -> {
                            WorkoutBuilderScreen(
                                initialWorkout = screen.initialWorkout,
                                onSaveWorkout = { workout ->
                                    coroutineScope.launch {
                                        appContainer.workoutRepository.saveWorkout(workout)
                                        currentScreen = Screen.Home
                                    }
                                },
                                onBackClicked = {
                                    currentScreen = Screen.Home
                                }
                            )
                        }

                        is Screen.Run -> {
                            val snapshot by (workoutService?.snapshot ?: remember { kotlinx.coroutines.flow.MutableStateFlow<RunnerSnapshot?>(null) })
                                .collectAsState()

                            LiveRunScreen(
                                snapshot = snapshot,
                                allSegments = screen.workout.flatten(),
                                onPauseClicked = { workoutService?.pauseWorkout() },
                                onResumeClicked = { workoutService?.resumeWorkout() },
                                onSkipClicked = { workoutService?.skipSegment() },
                                onStopConfirmed = { workoutService?.stopWorkout() }
                            )
                        }

                        is Screen.Results -> {
                            ResultsScreen(
                                session = screen.session,
                                onHomeClicked = {
                                    currentScreen = Screen.Home
                                }
                            )
                        }

                        is Screen.History -> {
                            HistoryScreen(
                                sessions = allSessions,
                                onSessionClicked = { session ->
                                    currentScreen = Screen.Results(session)
                                },
                                onBackClicked = {
                                    currentScreen = Screen.Home
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startWorkoutSession(workout: Workout) {
        val startIntent = WorkoutForegroundService.startIntent(this, workout)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(startIntent)
        } else {
            startService(startIntent)
        }
    }
}
