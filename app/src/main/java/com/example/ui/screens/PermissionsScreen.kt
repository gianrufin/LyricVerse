package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.LyricVerseUiState
import com.example.ui.theme.LyricActive
import com.example.ui.theme.StageObsidian
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningAmber

@Composable
fun PermissionsScreen(
    uiState: LyricVerseUiState,
    onOpenListenerSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleTracking: (Boolean) -> Unit,
    onToggleLockScreenNotif: (Boolean) -> Unit,
    onSimulateMissingMetadata: () -> Unit,
    onRefreshSessions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val perms = uiState.permissions
    val prefs = uiState.syncState.preferences
    val diag = uiState.diagnostics

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("permissions_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Permissions & Diagnostics",
                style = MaterialTheme.typography.headlineMedium,
                color = LyricActive
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Every permission is optional, user-controlled, and revocable anytime in Android Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        // Master tracking switch + Lock-screen notification preference
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StageSurfaceElevated),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Session Tracking Enabled",
                                style = MaterialTheme.typography.titleSmall,
                                color = LyricActive
                            )
                            Text(
                                text = "Stops cleanly when disabled, playback ends, or access is revoked.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = prefs.trackingEnabled,
                            onCheckedChange = onToggleTracking,
                            modifier = Modifier.testTag("tracking_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lock-Screen Lyric Notification",
                                style = MaterialTheme.typography.titleSmall,
                                color = LyricActive
                            )
                            Text(
                                text = "Best-effort ongoing notification line on lock screens (never bypasses keyguard).",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = prefs.lockScreenNotificationEnabled,
                            onCheckedChange = onToggleLockScreenNotif
                        )
                    }
                }
            }
        }

        // Permission checklist
        item {
            PermissionActionCard(
                icon = Icons.Rounded.NotificationsActive,
                title = "Notification Listener Access",
                granted = perms.notificationListenerGranted,
                reason = "Required by Android's MediaSessionManager to observe active track title, artist, and playback timestamps from other music apps. Does not alter audio.",
                revokeHint = "Revoke anytime in Settings → Apps → Special app access → Notification access.",
                actionLabel = if (perms.notificationListenerGranted) "Manage in Settings" else "Grant Access",
                testTag = "grant_listener_button",
                onAction = onOpenListenerSettings,
            )
        }

        item {
            PermissionActionCard(
                icon = Icons.Rounded.Layers,
                title = "Display Over Other Apps",
                granted = perms.overlayPermissionGranted,
                reason = "Needed only if you turn on the movable 2-line floating pill or expanded karaoke card over other apps.",
                revokeHint = "Revoke anytime in Settings → Display over other apps, or tap Close on the pill.",
                actionLabel = if (perms.overlayPermissionGranted) "Manage Overlay" else "Allow Overlay",
                testTag = "grant_overlay_button",
                onAction = onOpenOverlaySettings,
            )
        }

        item {
            PermissionActionCard(
                icon = Icons.Rounded.NotificationsActive,
                title = "Post Notifications (Android 13+)",
                granted = perms.postNotificationsGranted,
                reason = "Shows the ongoing foreground companion notification with Stop / Hide controls and optional lock-screen lyric line.",
                revokeHint = "Revoke anytime in System App Notification Settings.",
                actionLabel = if (perms.postNotificationsGranted) "Notification Settings" else "Allow Notifications",
                testTag = "grant_notifications_button",
                onAction = onRequestPostNotifications,
            )
        }

        item {
            PermissionActionCard(
                icon = Icons.Rounded.BatteryChargingFull,
                title = "Battery Optimization Guidance",
                granted = perms.ignoringBatteryOptimizations,
                reason = "Android and OEM battery savers may pause background listeners or overlays. LyricVerse never runs unrequested background tasks when tracking is off.",
                revokeHint = "Optional: choose Unrestricted battery usage only if your device stops active overlays.",
                actionLabel = "Battery Settings",
                testTag = "battery_settings_button",
                onAction = onOpenBatterySettings,
            )
        }

        // Live Session Diagnostics & Platform Limits
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StageSurfaceElevated),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Active Session Diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        color = LyricActive
                    )
                    Text(
                        text = "Detected sessions: ${diag.activeSessionsCount} · Selected: ${diag.selectedAppLabel ?: "None"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onRefreshSessions) {
                            Text("Refresh Sessions")
                        }
                        OutlinedButton(
                            onClick = onSimulateMissingMetadata,
                            modifier = Modifier.testTag("simulate_missing_metadata_button")
                        ) {
                            Text("Test Missing Metadata")
                        }
                    }
                }
            }
        }

        // Honest Android Auto & Lock-Screen Constraints
        item {
            Surface(
                color = StageObsidian.copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = TextMuted)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = "Lock-Screen Behavior: Best-Effort Only",
                            style = MaterialTheme.typography.titleSmall,
                            color = LyricActive
                        )
                    }
                    Text(
                        text = "LyricVerse never bypasses the keyguard or wakes the screen. On supported devices, lock-screen lyrics appear inside the ongoing companion notification depending on OEM and system privacy settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = TextMuted)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = "Android Auto Notice",
                            style = MaterialTheme.typography.titleSmall,
                            color = LyricActive
                        )
                    }
                    Text(
                        text = "Phone floating overlays do not render on Android Auto head units, and scrolling karaoke lyrics are excluded in car mode for driver safety. Audio playback continues undisturbed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionActionCard(
    icon: ImageVector,
    title: String,
    granted: Boolean,
    reason: String,
    revokeHint: String,
    actionLabel: String,
    testTag: String,
    onAction: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StageSurfaceElevated),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.padding(horizontal = 5.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = LyricActive
                    )
                }
                Icon(
                    imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (granted) "Granted" else "Not granted",
                    tint = if (granted) SuccessMint else WarningAmber
                )
            }
            Text(text = reason, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Text(text = revokeHint, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Button(
                onClick = onAction,
                modifier = Modifier.testTag(testTag)
            ) {
                Text(actionLabel)
            }
        }
    }
}
