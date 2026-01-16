package com.brunoafk.calendardnd.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.os.SystemClock
import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.data.prefs.RuntimeStateStore
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.ui.screens.CalendarScopeScreen
import com.brunoafk.calendardnd.ui.screens.CalendarPickerScreen
import com.brunoafk.calendardnd.ui.screens.DebugLogScreen
import com.brunoafk.calendardnd.ui.screens.IntroScreen
import com.brunoafk.calendardnd.ui.screens.LanguageScreen
import com.brunoafk.calendardnd.ui.screens.OnboardingScreen
import com.brunoafk.calendardnd.ui.screens.PrivacyScreen
import com.brunoafk.calendardnd.ui.screens.DndModeScreen
import com.brunoafk.calendardnd.ui.screens.DebugToolsScreen
import com.brunoafk.calendardnd.ui.screens.DebugLanguageScreen
import com.brunoafk.calendardnd.ui.screens.DebugLogSettingsScreen
import com.brunoafk.calendardnd.ui.screens.DebugSplashPreviewScreen
import com.brunoafk.calendardnd.ui.screens.PermissionsScreen
import com.brunoafk.calendardnd.ui.screens.AboutScreen
import com.brunoafk.calendardnd.ui.screens.WhatsNewScreen
import com.brunoafk.calendardnd.ui.screens.SettingsScreen
import com.brunoafk.calendardnd.ui.screens.StartupScreen
import com.brunoafk.calendardnd.ui.screens.StatusScreen
import com.brunoafk.calendardnd.ui.screens.UpdateScreen
import com.brunoafk.calendardnd.system.update.ManualUpdateManager
import com.brunoafk.calendardnd.util.debugTapLog
import com.brunoafk.calendardnd.util.navInteractionGate
import kotlinx.coroutines.launch

object AppRoutes {
    const val STARTUP = "startup"
    const val INTRO = "intro"
    const val LANGUAGE_ONBOARDING = "language_onboarding"
    const val LANGUAGE_SETTINGS = "language_settings"
    const val ONBOARDING = "onboarding"
    const val CALENDAR_SCOPE = "calendar_scope"
    const val PRIVACY = "privacy"
    const val STATUS = "status"
    const val SETTINGS = "settings?highlight={highlight}"
    const val PERMISSIONS = "permissions"
    const val ABOUT = "about"
    const val WHATS_NEW = "whats_new"
    const val DND_MODE = "dnd_mode"
    const val CALENDAR_PICKER = "calendar_picker"
    const val CALENDAR_PICKER_ONBOARDING = "calendar_picker_onboarding"
    const val DEBUG_LOGS = "debug_logs"
    const val HELP = "help"
    const val DEBUG_TOOLS = "debug_tools"
    const val DEBUG_LANGUAGE = "debug_language/{tag}"
    const val DEBUG_SPLASH = "debug_splash"
    const val DEBUG_LOG_SETTINGS = "debug_log_settings"
    const val UPDATES = "updates"
}

@Composable
fun AppNavigation(
    showTileHint: Boolean = false,
    onTileHintConsumed: () -> Unit = {},
    updateStatus: ManualUpdateManager.UpdateStatus? = null,
    openUpdates: Boolean = false,
    onOpenUpdatesConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val settingsStore = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    var lastNavEventMs by remember { mutableStateOf(0L) }
    val debugOverlayEnabled by settingsStore.debugOverlayEnabled.collectAsState(
        initial = false
    )
    val currentRoute = navBackStackEntry?.destination?.route.orEmpty()

    // Track locked routes - use a Set to handle multiple locked routes during transitions
    val lockedRoutes = remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(navBackStackEntry?.destination?.route) {
        lastNavEventMs = SystemClock.elapsedRealtime()
    }

    LaunchedEffect(openUpdates, currentRoute, updateStatus) {
        if (openUpdates && currentRoute.isNotBlank() && currentRoute != AppRoutes.STARTUP) {
            navController.navigate(AppRoutes.UPDATES) {
                launchSingleTop = true
            }
            updateStatus?.let { status ->
                settingsStore.setLastSeenUpdateVersion(status.info.versionName)
            }
            onOpenUpdatesConsumed()
        }
    }

    Box {
        NavHost(
            navController = navController,
            startDestination = AppRoutes.STARTUP
        ) {
            composable(AppRoutes.STARTUP) {
                val route = AppRoutes.STARTUP
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes
                ) {
                    StartupScreen(
                        onGoIntro = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.LANGUAGE_ONBOARDING) {
                                popUpTo(AppRoutes.STARTUP) { inclusive = true }
                            }
                        },
                        onGoPermissions = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.ONBOARDING) {
                                popUpTo(AppRoutes.STARTUP) { inclusive = true }
                            }
                        },
                        onGoStatus = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.STATUS) {
                                popUpTo(AppRoutes.STARTUP) { inclusive = true }
                            }
                        }
                    )
                }
            }
            composable(AppRoutes.INTRO) {
                val route = AppRoutes.INTRO
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    IntroScreen(
                        onContinue = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.ONBOARDING)
                        }
                    )
                }
            }
            composable(AppRoutes.LANGUAGE_ONBOARDING) {
                val route = AppRoutes.LANGUAGE_ONBOARDING
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    LanguageScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        },
                        onContinue = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.INTRO)
                        },
                        showBack = false,
                        showContinue = true
                    )
                }
            }
            composable(AppRoutes.LANGUAGE_SETTINGS) {
                val route = AppRoutes.LANGUAGE_SETTINGS
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    LanguageScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        },
                        onContinue = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        },
                        showBack = true,
                        showContinue = false
                    )
                }
            }
            composable(AppRoutes.ONBOARDING) {
                val route = AppRoutes.ONBOARDING
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    OnboardingScreen(
                        onContinue = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.CALENDAR_SCOPE)
                        }
                    )
                }
            }
            composable(AppRoutes.CALENDAR_SCOPE) {
                val route = AppRoutes.CALENDAR_SCOPE
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    CalendarScopeScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        },
                        onSelectAllCalendars = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.PRIVACY)
                        },
                        onSelectSpecificCalendars = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.CALENDAR_PICKER_ONBOARDING)
                        }
                    )
                }
            }
            composable(AppRoutes.PRIVACY) {
                val route = AppRoutes.PRIVACY
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    PrivacyScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        },
                        onContinue = {
                            lockedRoutes.value = lockedRoutes.value + route
                            scope.launch {
                                settingsStore.setOnboardingCompleted(true)
                            }
                            navController.navigate(AppRoutes.STATUS) {
                                popUpTo(AppRoutes.STARTUP) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
            composable(AppRoutes.STATUS) {
                val route = AppRoutes.STATUS
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes
                ) {
                    StatusScreen(
                        showTileHint = showTileHint,
                        onTileHintDismissed = onTileHintConsumed,
                        updateStatus = updateStatus,
                        onOpenUpdates = {
                            updateStatus?.let { status ->
                                scope.launch {
                                    settingsStore.setLastSeenUpdateVersion(status.info.versionName)
                                }
                            }
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.UPDATES)
                        },
                        onOpenSettings = { highlight ->
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate("settings?highlight=${if (highlight) "1" else "0"}")
                        },
                        onOpenDebugLogs = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.DEBUG_LOGS)
                        },
                        onOpenSetup = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.ONBOARDING)
                        },
                        onOpenDndMode = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.DND_MODE)
                        }
                    )
                }
            }
            composable(
                route = AppRoutes.SETTINGS,
                arguments = listOf(navArgument("highlight") {
                    type = NavType.StringType
                    defaultValue = "0"
                })
            ) { backStackEntry ->
                val route = AppRoutes.SETTINGS
                val highlight = backStackEntry.arguments?.getString("highlight") == "1"
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    SettingsScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        },
                        onNavigateToCalendarPicker = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.CALENDAR_PICKER)
                        },
                        onNavigateToHelp = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.HELP)
                        },
                        onNavigateToAbout = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.ABOUT)
                        },
                        onNavigateToLanguage = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.LANGUAGE_SETTINGS)
                        },
                        onNavigateToDebugTools = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.DEBUG_TOOLS)
                        },
                        onNavigateToUpdates = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.UPDATES)
                        },
                        onNavigateToDndMode = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.DND_MODE)
                        },
                        onNavigateToPermissions = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.PERMISSIONS)
                        },
                        highlightAutomation = highlight
                    )
                }
            }
            composable(AppRoutes.PERMISSIONS) {
                val route = AppRoutes.PERMISSIONS
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    PermissionsScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(AppRoutes.ABOUT) {
                val route = AppRoutes.ABOUT
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    AboutScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        },
                        onNavigateToWhatsNew = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.WHATS_NEW)
                        }
                    )
                }
            }
            composable(AppRoutes.WHATS_NEW) {
                val route = AppRoutes.WHATS_NEW
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    WhatsNewScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(AppRoutes.DND_MODE) {
                val route = AppRoutes.DND_MODE
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    DndModeScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(AppRoutes.CALENDAR_PICKER) {
                val route = AppRoutes.CALENDAR_PICKER
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    CalendarPickerScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        },
                        isActive = navBackStackEntry?.destination?.route == AppRoutes.CALENDAR_PICKER
                    )
                }
            }
            composable(AppRoutes.CALENDAR_PICKER_ONBOARDING) {
                val route = AppRoutes.CALENDAR_PICKER_ONBOARDING
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    CalendarPickerScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        },
                        onDone = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.PRIVACY)
                        },
                        isActive = navBackStackEntry?.destination?.route == AppRoutes.CALENDAR_PICKER_ONBOARDING
                    )
                }
            }
            composable(AppRoutes.DEBUG_LOGS) {
                val route = AppRoutes.DEBUG_LOGS
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    DebugLogScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(AppRoutes.HELP) {
                val route = AppRoutes.HELP
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    com.brunoafk.calendardnd.ui.screens.HelpScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(AppRoutes.DEBUG_TOOLS) {
                val route = AppRoutes.DEBUG_TOOLS
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    DebugToolsScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        },
                        onOpenLanguage = { tag ->
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate("debug_language/$tag")
                        },
                        onOpenDebugLogs = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.DEBUG_LOGS)
                        }
                        ,
                        onOpenLogSettings = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.navigate(AppRoutes.DEBUG_LOG_SETTINGS)
                        }
                    )
                }
            }
            composable(AppRoutes.DEBUG_LOG_SETTINGS) {
                val route = AppRoutes.DEBUG_LOG_SETTINGS
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    DebugLogSettingsScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(
                route = AppRoutes.DEBUG_LANGUAGE,
                arguments = listOf(navArgument("tag") { type = NavType.StringType })
            ) { backStackEntry ->
                val tag = backStackEntry.arguments?.getString("tag").orEmpty()
                val routeLabel = backStackEntry.destination.route ?: AppRoutes.DEBUG_LANGUAGE
                val route = routeLabel
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    DebugLanguageScreen(
                        languageTag = tag,
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(AppRoutes.DEBUG_SPLASH) {
                val route = AppRoutes.DEBUG_SPLASH
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    DebugSplashPreviewScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(AppRoutes.UPDATES) {
                val route = AppRoutes.UPDATES
                DestinationWrapper(
                    route = route,
                    currentRoute = currentRoute,
                    debugOverlayEnabled = debugOverlayEnabled,
                    lockedRoutes = lockedRoutes,
                    onSystemBack = {
                        lockedRoutes.value = lockedRoutes.value + route
                        navController.popBackStack()
                    }
                ) {
                    UpdateScreen(
                        onNavigateBack = {
                            lockedRoutes.value = lockedRoutes.value + route
                            navController.popBackStack()
                        }
                    )
                }
            }
        }

        if (debugOverlayEnabled && (BuildConfig.DEBUG || BuildConfig.DEBUG_TOOLS_ENABLED)) {
            DebugNavOverlay(
                modifier = Modifier.align(Alignment.TopStart),
                currentRoute = navBackStackEntry?.destination?.route.orEmpty(),
                lastNavEventMs = lastNavEventMs
            )
        }
    }
}

@Composable
private fun DestinationWrapper(
    route: String,
    currentRoute: String,
    debugOverlayEnabled: Boolean,
    lockedRoutes: androidx.compose.runtime.MutableState<Set<String>>,
    onSystemBack: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var wasCurrent by remember { mutableStateOf(false) }

    DisposableEffect(route) {
        onDispose {
            lockedRoutes.value = lockedRoutes.value - route
        }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute == route) {
            if (!wasCurrent) {
                lockedRoutes.value = lockedRoutes.value - route
            }
            wasCurrent = true
        } else {
            wasCurrent = false
        }
    }

    val isInteractive = currentRoute == route && !lockedRoutes.value.contains(route)
    if (onSystemBack != null) {
        BackHandler(enabled = isInteractive) {
            onSystemBack()
        }
    }
    val useDebug = debugOverlayEnabled && (BuildConfig.DEBUG || BuildConfig.DEBUG_TOOLS_ENABLED)
    val modifier = if (useDebug) {
        Modifier
            .debugTapLog(route, isInteractive)  // Log with interactive state
            .navInteractionGate(isInteractive)   // Then gate
    } else {
        Modifier.navInteractionGate(isInteractive)
    }

    Box(modifier = modifier) {
        content()
    }
}

@Composable
private fun DebugNavOverlay(
    modifier: Modifier,
    currentRoute: String,
    lastNavEventMs: Long
) {
    if (!BuildConfig.DEBUG && !BuildConfig.DEBUG_TOOLS_ENABLED) {
        return
    }

    var expanded by remember { mutableStateOf(true) }
    val elapsedMs = if (lastNavEventMs == 0L) {
        "--"
    } else {
        (SystemClock.elapsedRealtime() - lastNavEventMs).toString()
    }
    val label = if (expanded) {
        "NAV DEBUG\nroute=$currentRoute\nlast=${elapsedMs}ms\n(tap to collapse)"
    } else {
        "NAV DEBUG (tap)"
    }

    Surface(
        modifier = modifier
            .padding(8.dp)
            .clickable { expanded = !expanded },
        color = MaterialTheme.colorScheme.tertiaryContainer,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary),
        tonalElevation = 10.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}
