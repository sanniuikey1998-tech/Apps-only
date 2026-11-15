package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.DangerousShortcutDialog
import com.example.ui.components.SpatialBackground
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.KeyboardScreen
import com.example.ui.screens.MediaPresentationScreen
import com.example.ui.screens.MouseTouchpadScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShortcutsScreen
import com.example.ui.theme.DeckLinkTheme
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.SpaceDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

object DeckLinkRoutes {
    const val HOME = "home"
    const val MOUSE = "mouse"
    const val KEYBOARD = "keyboard"
    const val MEDIA = "media"
    const val SHORTCUTS = "shortcuts"
    const val SETTINGS = "settings"
}

private data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val mainNavItems = listOf(
    NavItem(DeckLinkRoutes.HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    NavItem(DeckLinkRoutes.MOUSE, "Mouse", Icons.Rounded.Mouse, Icons.Outlined.Mouse),
    NavItem(DeckLinkRoutes.KEYBOARD, "Keyboard", Icons.Rounded.Keyboard, Icons.Outlined.Keyboard),
    NavItem(DeckLinkRoutes.MEDIA, "Media", Icons.Rounded.PlayCircle, Icons.Outlined.PlayCircle),
    NavItem(DeckLinkRoutes.SHORTCUTS, "Shortcuts", Icons.Rounded.Bolt, Icons.Outlined.Bolt),
    NavItem(DeckLinkRoutes.SETTINGS, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings)
)

@Composable
fun DeckLinkApp(
    viewModel: DeckLinkViewModel = viewModel()
) {
    val hidStatus by viewModel.hidStatus.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    val discoveredBt by viewModel.discoveredBtDevices.collectAsStateWithLifecycle()
    val isBtDiscovering by viewModel.isBtDiscovering.collectAsStateWithLifecycle()
    val wifiStatus by viewModel.wifiStatus.collectAsStateWithLifecycle()
    val discoveredWifiPcs by viewModel.discoveredWifiPcs.collectAsStateWithLifecycle()
    val isWifiScanning by viewModel.isWifiScanning.collectAsStateWithLifecycle()
    val batteryStatus by viewModel.batteryStatus.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val latchedModifiers by viewModel.latchedModifiers.collectAsStateWithLifecycle()
    val lockedModifiers by viewModel.lockedModifiers.collectAsStateWithLifecycle()
    val dragLockEnabled by viewModel.dragLockEnabled.collectAsStateWithLifecycle()
    val laserPointerActive by viewModel.laserPointerActive.collectAsStateWithLifecycle()
    val pendingShortcut by viewModel.pendingDangerousShortcut.collectAsStateWithLifecycle()

    var mediaInitialTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: DeckLinkRoutes.HOME

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissionsAndHid()
    }

    LaunchedEffect(Unit) {
        viewModel.feedback.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    DeckLinkTheme {
        SpatialBackground {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 640.dp

                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useNavigationRail) {
                            NavigationBar(
                                containerColor = SpaceDeep.copy(alpha = 0.94f),
                                tonalElevation = 0.dp
                            ) {
                                mainNavItems.forEach { item ->
                                    val selected = currentRoute == item.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            if (item.route == DeckLinkRoutes.MEDIA) mediaInitialTab = 0
                                            navigateTab(item.route)
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.label,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Clip
                                            )
                                        },
                                        alwaysShowLabel = true,
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ElectricBlueBright,
                                            selectedTextColor = ElectricBlueBright,
                                            indicatorColor = ElectricBlue.copy(alpha = 0.22f),
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted
                                        ),
                                        modifier = Modifier.testTag("nav_${item.route}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavigationRail) {
                            NavigationRail(
                                containerColor = SpaceDeep.copy(alpha = 0.92f)
                            ) {
                                mainNavItems.forEach { item ->
                                    val selected = currentRoute == item.route
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = {
                                            if (item.route == DeckLinkRoutes.MEDIA) mediaInitialTab = 0
                                            navigateTab(item.route)
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = { Text(item.label, maxLines = 1) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = ElectricBlueBright,
                                            selectedTextColor = ElectricBlueBright,
                                            indicatorColor = ElectricBlue.copy(alpha = 0.22f),
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted
                                        ),
                                        modifier = Modifier.testTag("rail_${item.route}")
                                    )
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = DeckLinkRoutes.HOME,
                            modifier = Modifier.weight(1f)
                        ) {
                            composable(DeckLinkRoutes.HOME) {
                                HomeDashboardScreen(
                                    hidStatus = hidStatus,
                                    pairedDevices = pairedDevices,
                                    wifiStatus = wifiStatus,
                                    batteryStatus = batteryStatus,
                                    permissions = permissions,
                                    hapticsEnabled = preferences.hapticFeedback,
                                    onQuickConnectClick = viewModel::connectOrQuickConnect,
                                    onConnectDevice = viewModel::connectToDevice,
                                    onRequestPermissions = {
                                        permissionLauncher.launch(viewModel.requiredRuntimePermissions())
                                    },
                                    onOpenMode = { mode ->
                                        when (mode) {
                                            "mouse" -> navigateTab(DeckLinkRoutes.MOUSE)
                                            "keyboard" -> navigateTab(DeckLinkRoutes.KEYBOARD)
                                            "media" -> {
                                                mediaInitialTab = 0
                                                navigateTab(DeckLinkRoutes.MEDIA)
                                            }
                                            "presentation" -> {
                                                mediaInitialTab = 1
                                                navigateTab(DeckLinkRoutes.MEDIA)
                                            }
                                            "shortcuts" -> navigateTab(DeckLinkRoutes.SHORTCUTS)
                                        }
                                    },
                                    onOpenConnectionSettings = { navigateTab(DeckLinkRoutes.SETTINGS) }
                                )
                            }

                            composable(DeckLinkRoutes.MOUSE) {
                                MouseTouchpadScreen(
                                    hidStatus = hidStatus,
                                    preferences = preferences,
                                    dragLockEnabled = dragLockEnabled,
                                    onMovePointer = viewModel::movePointer,
                                    onResetMath = viewModel::resetTouchpadAccumulators,
                                    onScrollTwoFinger = viewModel::scrollTwoFinger,
                                    onLeftClick = { viewModel.clickMouseButton(com.example.domain.hid.HidKeyCodes.MOUSE_LEFT) },
                                    onRightClick = { viewModel.clickMouseButton(com.example.domain.hid.HidKeyCodes.MOUSE_RIGHT) },
                                    onMiddleClick = { viewModel.clickMouseButton(com.example.domain.hid.HidKeyCodes.MOUSE_MIDDLE) },
                                    onDoubleClick = viewModel::doubleClickLeft,
                                    onButtonHoldChanged = viewModel::setMouseButtonHeld,
                                    onToggleDragLock = viewModel::toggleDragLock,
                                    onSetDragMode = viewModel::setDragMode,
                                    onThreeFingerGesture = viewModel::executeThreeFingerGesture,
                                    onFourFingerGesture = viewModel::executeFourFingerGesture,
                                    onPointerSpeedChange = viewModel::setPointerSpeed,
                                    onSensitivityChange = viewModel::setMouseSensitivity,
                                    onNaturalScrollChange = viewModel::setNaturalScrolling,
                                    onOpenKeyboard = { navigateTab(DeckLinkRoutes.KEYBOARD) },
                                    onOpenPresentation = {
                                        mediaInitialTab = 1
                                        navigateTab(DeckLinkRoutes.MEDIA)
                                    }
                                )
                            }

                            composable(DeckLinkRoutes.KEYBOARD) {
                                KeyboardScreen(
                                    hidStatus = hidStatus,
                                    latchedModifiers = latchedModifiers,
                                    lockedModifiers = lockedModifiers,
                                    customShortcuts = preferences.customShortcuts,
                                    hapticsEnabled = preferences.hapticFeedback,
                                    onToggleModifier = viewModel::toggleModifierLatch,
                                    onClearModifiers = viewModel::clearModifiers,
                                    onSendKey = viewModel::sendKeyboardKey,
                                    onSendChar = viewModel::sendCharFromTextBar,
                                    onSendText = viewModel::sendTextString,
                                    onTriggerCustomShortcut = viewModel::triggerCustomShortcut
                                )
                            }

                            composable(DeckLinkRoutes.MEDIA) {
                                MediaPresentationScreen(
                                    hidStatus = hidStatus,
                                    initialTab = mediaInitialTab,
                                    laserPointerActive = laserPointerActive,
                                    hapticsEnabled = preferences.hapticFeedback,
                                    onSendConsumerKey = viewModel::sendMediaConsumerKey,
                                    onPresentationAction = viewModel::sendPresentationAction,
                                    onToggleLaserPointer = viewModel::toggleLaserPointer,
                                    onMovePointer = viewModel::movePointer,
                                    onLeftClick = { viewModel.clickMouseButton(com.example.domain.hid.HidKeyCodes.MOUSE_LEFT) }
                                )
                            }

                            composable(DeckLinkRoutes.SHORTCUTS) {
                                ShortcutsScreen(
                                    hidStatus = hidStatus,
                                    customShortcuts = preferences.customShortcuts,
                                    hapticsEnabled = preferences.hapticFeedback,
                                    onTriggerShortcut = viewModel::triggerWindowsShortcut,
                                    onTriggerCustomShortcut = viewModel::triggerCustomShortcut,
                                    onAddCustomShortcut = viewModel::addCustomShortcut,
                                    onDeleteCustomShortcut = viewModel::deleteCustomShortcut
                                )
                            }

                            composable(DeckLinkRoutes.SETTINGS) {
                                SettingsScreen(
                                    hidStatus = hidStatus,
                                    isHidApiSupported = viewModel.isHidApiSupported,
                                    permissions = permissions,
                                    pairedDevices = pairedDevices,
                                    discoveredBtDevices = discoveredBt,
                                    isBtDiscovering = isBtDiscovering,
                                    wifiStatus = wifiStatus,
                                    discoveredWifiPcs = discoveredWifiPcs,
                                    isWifiScanning = isWifiScanning,
                                    preferences = preferences,
                                    onRequestPermissions = {
                                        permissionLauncher.launch(viewModel.requiredRuntimePermissions())
                                    },
                                    onConnectDevice = viewModel::connectToDevice,
                                    onDisconnect = viewModel::disconnect,
                                    onStartBtDiscovery = viewModel::startBtDiscovery,
                                    onStopBtDiscovery = viewModel::stopBtDiscovery,
                                    onPairBtDevice = viewModel::pairBtDevice,
                                    onScanWifiLan = viewModel::scanWifiLan,
                                    onSendWakeOnLan = viewModel::sendWakeOnLan,
                                    onPointerSpeedChange = viewModel::setPointerSpeed,
                                    onSensitivityChange = viewModel::setMouseSensitivity,
                                    onNaturalScrollChange = viewModel::setNaturalScrolling,
                                    onHapticsChange = viewModel::setHapticFeedback,
                                    onAutoReconnectChange = viewModel::setAutoReconnect,
                                    onAutoConnectConnectedChange = viewModel::setAutoConnectConnectedDevices,
                                    onDeviceAutoConnectChange = viewModel::setDeviceAutoConnect,
                                    onTimeoutChange = viewModel::setConnectionTimeout,
                                    onForegroundServiceChange = viewModel::setForegroundServiceEnabled,
                                    onConfirmDangerousChange = viewModel::setConfirmDangerousShortcuts,
                                    onFourFingerGestureChange = viewModel::setFourFingerGesture
                                )
                            }
                        }
                    }
                }
            }

            pendingShortcut?.let { shortcut ->
                DangerousShortcutDialog(
                    shortcut = shortcut,
                    onConfirm = viewModel::confirmPendingShortcut,
                    onDismiss = viewModel::dismissPendingShortcut
                )
            }
        }
    }
}
