/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import com.agupta07505.smartisland.ui.IslandText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.ui.rememberRecordingSeconds
import com.agupta07505.smartisland.util.TimerStopwatchParser
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun StopwatchExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit = {},
    onCollapse: () -> Unit = {},
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val context = LocalContext.current
    val stopwatchColor = Color(settings.stopwatchColor)

    // The listener keeps the stopwatch clock: a paused card carries its frozen time, a running
    // one counts from its start. Nothing here is recomputed from the wall clock while paused.
    val frozenMs = notification?.recordingFrozenMs

    // Optimistic freeze after tapping Pause, until the app posts its paused notification.
    var tappedPauseAtSec by remember(notification?.key, frozenMs) { mutableLongStateOf(-1L) }
    val isPaused = frozenMs != null || tappedPauseAtSec >= 0L

    val liveSeconds = rememberRecordingSeconds(notification)
    val elapsedSeconds = if (tappedPauseAtSec >= 0L) tappedPauseAtSec else liveSeconds

    val displayTime = remember(elapsedSeconds) {
        TimerStopwatchParser.formatTime(elapsedSeconds)
    }

    // No rotation runs while the card waits collapsed (pre-composed): the transition only exists while the card is shown.
    val rotation = if (com.agupta07505.smartisland.ui.LocalCardShown.current) {
        val infiniteTransition = rememberInfiniteTransition(label = "stopwatchAnim")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "stopwatchRotation"
        )
        animated
    } else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenNotification() }
            .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Stopwatch Glyph & Time
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(stopwatchColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AvTimer,
                    contentDescription = "Stopwatch",
                    tint = stopwatchColor,
                    modifier = Modifier
                        .size(22.dp)
                        .then(if (!isPaused) Modifier.graphicsLayer { rotationZ = rotation } else Modifier)
                )
            }

            Column {
                Text(
                    text = displayTime,
                    color = LocalIslandPalette.current.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
                val notifText = notification?.text.orEmpty()
                val notifTitle = notification?.title.orEmpty()
                val subtitle = if (isPaused) {
                    "Paused"
                } else if (notifText.isNotBlank() && !notifText.equals("Stopwatch", ignoreCase = true) && !notifText.contains("00:")) {
                    notifText
                } else if (notifTitle.isNotBlank() && !notifTitle.equals("Stopwatch", ignoreCase = true) && !notifTitle.contains("00:")) {
                    notifTitle
                } else {
                    "Stopwatch Active"
                }
                Text(
                    text = subtitle,
                    color = stopwatchColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Right: Interactive Quick Actions (Lap, Pause/Resume, Reset)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Lap: only shown when the stopwatch app offers a Lap button. The app owns the lap list.
            val lapAction = notification.actionWithRole(ActionRole.Lap)
            if (lapAction != null && notification != null && !isPaused) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LocalIslandPalette.current.pick(Color(0xFF27272A), Color(0xFFD9D8DB)))
                        .bounceClick { notification.fireAction(context, lapAction) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Flag,
                        contentDescription = lapAction.title,
                        tint = LocalIslandPalette.current.pick(Color(0xFFE4E4E7), Color(0xFF1F2937)),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 2. Pause / Resume: the app's own button.
            val toggleAction = if (isPaused) notification.actionWithRole(ActionRole.Resume)
            else notification.actionWithRole(ActionRole.Pause)
            if (toggleAction != null && notification != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(stopwatchColor)
                        .bounceClick {
                            if (notification.fireAction(context, toggleAction) && !isPaused) {
                                tappedPauseAtSec = elapsedSeconds
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = toggleAction.title,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 3. Reset / Stop: the app's own button, then clear the island card.
            val resetAction = notification.actionWithRole(ActionRole.Reset, ActionRole.Stop)
            if (resetAction != null && notification != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LocalIslandPalette.current.pick(Color(0xFF3F3F46), Color(0xFFC9C8CB)))
                        .bounceClick {
                            notification.fireAction(context, resetAction)
                            SmartIslandRepositories.notificationRepository(context).removeNotification(notification.key)
                            onCollapse()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = resetAction.title,
                        tint = LocalIslandPalette.current.pick(Color(0xFFE4E4E7), Color(0xFF1F2937)),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 4. Any other button the stopwatch app put in its notification, shown as the app labels it.
            if (notification != null) {
                notification.leftoverActions(lapAction, toggleAction, resetAction).forEach {
                    ShadeActionChip(notification, it, stopwatchColor)
                }
            }
        }
    }
}
