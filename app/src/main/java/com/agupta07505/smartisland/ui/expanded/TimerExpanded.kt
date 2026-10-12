/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import com.agupta07505.smartisland.ui.IslandText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.TimerStopwatchParser
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun TimerExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit = {},
    onCollapse: () -> Unit = {},
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val context = LocalContext.current
    val timerColor = Color(settings.timerColor)

    val pausedKeywords = remember {
        listOf("paused", "pause", "en pause", "pausado", "pausada", "angehalten", "sospeso", "sospesa", "रोक दिया गया", "已暂停", "一時停止中", "приостановлено")
    }

    // The notification is the source of truth: a "Resume" button or "paused" wording means paused.
    val isNotificationPaused = remember(notification?.key, notification?.actionIntents, notification?.text, notification?.title) {
        notification.offersResume() || pausedKeywords.any {
            notification?.text?.contains(it, ignoreCase = true) == true ||
            notification?.title?.contains(it, ignoreCase = true) == true
        }
    }

    // Optimistic flip after a tap; resets as soon as the app posts its updated notification.
    var isPaused by remember(notification?.key, isNotificationPaused) {
        mutableStateOf(isNotificationPaused)
    }

    var targetTime by remember(notification?.key, notification?.timeMillis) {
        mutableLongStateOf(notification?.timeMillis ?: (System.currentTimeMillis() + 300000L))
    }

    var remainingSec by remember(notification?.key) {
        val parsed = notification?.let { TimerStopwatchParser.parseTimerRemainingSeconds(it) }
        val rem = if (parsed != null && parsed > 0) {
            parsed
        } else {
            val t = notification?.timeMillis ?: (System.currentTimeMillis() + 300000L)
            if (t > System.currentTimeMillis()) {
                ((t - System.currentTimeMillis() + 500L) / 1000L).coerceAtLeast(0L)
            } else {
                0L
            }
        }
        mutableLongStateOf(rem)
    }

    // The paused remaining time the listener froze; the text of a paused timer can still show its full length.
    val frozenSec = if (isPaused && notification?.recordingPaused == true) {
        notification.recordingFrozenMs?.let { (it + 500L) / 1000L }
    } else {
        null
    }

    LaunchedEffect(notification?.text, notification?.title, frozenSec) {
        if (isPaused && notification != null) {
            if (frozenSec != null) {
                remainingSec = frozenSec
            } else {
                val parsed = TimerStopwatchParser.parseTimerRemainingSeconds(notification)
                if (parsed != null && parsed > 0) {
                    remainingSec = parsed
                }
            }
        }
    }

    LaunchedEffect(notification?.key, targetTime, isPaused) {
        if (!isPaused) {
            while (true) {
                val now = System.currentTimeMillis()
                val rem = if (targetTime > now) {
                    ((targetTime - now + 500L) / 1000L).coerceAtLeast(0L)
                } else {
                    0L
                }
                remainingSec = rem
                if (rem <= 0L) break
                kotlinx.coroutines.delay(500L)
            }
        }
    }

    val shownSec = frozenSec ?: remainingSec
    val displayTime = remember(shownSec) {
        TimerStopwatchParser.formatTime(shownSec)
    }

    // A card that waits collapsed (pre-composed) has no pulse running: the transition only exists while the card is shown.
    val pulseScale = if (com.agupta07505.smartisland.ui.LocalCardShown.current) {
        val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "timerPulseScale"
        )
        animated
    } else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenNotification() }
            .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Hourglass / Timer Glyph
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(timerColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.HourglassBottom,
                    contentDescription = "Timer",
                    tint = timerColor,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            if (!isPaused && remainingSec > 0) {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                        }
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
                val timerSubtitle = if (remainingSec == 0L && !isPaused) {
                    "Time's up!"
                } else if (isPaused) {
                    "Paused"
                } else {
                    val notifTitle = notification?.title.orEmpty()
                    val notifText = notification?.text.orEmpty()
                    if (notifTitle.isNotBlank() && !notifTitle.equals("Timer", ignoreCase = true) && !notifTitle.matches(ClockOnlyRegex)) {
                        notifTitle
                    } else if (notifText.isNotBlank() && !notifText.equals("Timer", ignoreCase = true) && !notifText.matches(ClockOnlyRegex)) {
                        notifText
                    } else {
                        "Timer Active"
                    }
                }
                Text(
                    text = timerSubtitle,
                    color = if (remainingSec == 0L && !isPaused) LocalIslandPalette.current.danger else timerColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Right: Interactive Quick Actions (Pause/Resume, +1 min, Stop/Reset)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Pause / Resume: only shown when the timer app offers that button.
            val toggleAction = if (isPaused) notification.actionWithRole(ActionRole.Resume)
            else notification.actionWithRole(ActionRole.Pause)

            if (toggleAction != null && notification != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(timerColor)
                        .bounceClick {
                            if (notification.fireAction(context, toggleAction)) {
                                val newPaused = !isPaused
                                isPaused = newPaused
                                targetTime = System.currentTimeMillis() + remainingSec * 1000L
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = toggleAction.title,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. +1 min: only shown when the timer app offers its own add-a-minute button.
            val addMinuteAction = notification.actionWithRole(ActionRole.AddMinute)
            if (addMinuteAction != null && notification != null) {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .defaultMinSize(minWidth = 40.dp)
                        .clip(CircleShape)
                        .background(timerColor.copy(alpha = 0.2f))
                        .bounceClick {
                            if (notification.fireAction(context, addMinuteAction)) {
                                // Show the extra minute right away; the app's re-posted notification confirms it.
                                if (isPaused) {
                                    remainingSec += 60L
                                } else {
                                    targetTime = maxOf(targetTime, System.currentTimeMillis()) + 60_000L
                                    remainingSec += 60L
                                }
                            }
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+1 min",
                        color = timerColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            // 3. Stop / Reset / Cancel: fires the app's own button, then clears the island card.
            val stopAction = notification.actionWithRole(ActionRole.Stop, ActionRole.Reset)
            if (stopAction != null && notification != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LocalIslandPalette.current.pick(Color(0xFF27272A), Color(0xFFD9D8DB)))
                        .bounceClick {
                            notification.fireAction(context, stopAction)
                            SmartIslandRepositories.notificationRepository(context).removeNotification(notification.key)
                            onCollapse()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = stopAction.title,
                        tint = LocalIslandPalette.current.danger,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 4. Any other button the timer app put in its notification, shown as the app labels it.
            if (notification != null) {
                notification.leftoverActions(toggleAction, addMinuteAction, stopAction).forEach {
                    ShadeActionChip(notification, it, timerColor)
                }
            }
        }
    }
}

/** Text that is only a clock reading (digits, colons, dots); built once. */
private val ClockOnlyRegex = Regex("""^[\d:.]+$""")
