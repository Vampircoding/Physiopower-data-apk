package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

enum class Screen {
    SPLASH,
    HOME,
    GROUP_DETAIL,
    OCR,
    ANALYTICS,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val darkTheme by viewModel.darkThemeState.collectAsState()

            MyApplicationTheme(darkTheme = darkTheme) {
                var currentScreen by remember { mutableStateOf(Screen.SPLASH) }

                // Observe selected split contexts
                val groups by viewModel.allGroups.collectAsState()
                val selectedGroup by viewModel.selectedGroup.collectAsState()
                val currentMembers by viewModel.currentMembers.collectAsState()
                val currentExpenses by viewModel.currentExpenses.collectAsState()
                val groupSummary by viewModel.groupSummary.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Animated screen crossfade for elite visuals
                    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            Screen.SPLASH -> {
                                SplashScreen(
                                    onDismiss = { currentScreen = Screen.HOME }
                                )
                            }

                            Screen.HOME -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    groups = groups,
                                    onNavigateToGroup = { id ->
                                        viewModel.selectGroup(id)
                                        currentScreen = Screen.GROUP_DETAIL
                                    },
                                    onNavigateToOcr = { currentScreen = Screen.OCR },
                                    onNavigateToSettings = { currentScreen = Screen.SETTINGS }
                                )
                            }

                            Screen.GROUP_DETAIL -> {
                                selectedGroup?.let { gp ->
                                    GroupDetailScreen(
                                        viewModel = viewModel,
                                        group = gp,
                                        members = currentMembers,
                                        expenses = currentExpenses,
                                        summary = groupSummary,
                                        onNavigateToAnalytics = { currentScreen = Screen.ANALYTICS },
                                        onBack = {
                                            viewModel.selectGroup(null)
                                            currentScreen = Screen.HOME
                                        }
                                    )
                                } ?: run {
                                    currentScreen = Screen.HOME
                                }
                            }

                            Screen.OCR -> {
                                OcrScannerScreen(
                                    viewModel = viewModel,
                                    groups = groups,
                                    onBack = { currentScreen = Screen.HOME }
                                )
                            }

                            Screen.ANALYTICS -> {
                                selectedGroup?.let { gp ->
                                    AnalyticsScreen(
                                        group = gp,
                                        summary = groupSummary,
                                        expenses = currentExpenses,
                                        onBack = { currentScreen = Screen.GROUP_DETAIL }
                                    )
                                } ?: run {
                                    currentScreen = Screen.HOME
                                }
                            }

                            Screen.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = Screen.HOME }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
