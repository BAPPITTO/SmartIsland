/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.agupta07505.smartisland.util.DndModeReader
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgetOff
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.ui.search.searchAnchor
import kotlinx.coroutines.launch

/**
 * Settings > Idle widgets. Three cards:
 *  - the switches, one set for portrait and one for landscape (the Portrait / Landscape tabs choose which one is shown): show idle widgets,
 *    turn them off after a tap on the cutout closed the island, turn them off while a long press has the pill hidden;
 *  - the widget selector, like the App shortcuts one but every widget can be ticked;
 *  - the two timers, shared by both orientations: the sideways tick of the glyphs and the automatic widget switch.
 */
@Composable
fun IdleWidgetsSettingsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(OrientationTab.Portrait) }
    val landscape = tab == OrientationTab.Landscape
    var showSelector by rememberSaveable { mutableStateOf(false) }

    val enabled = if (landscape) settings.idleWidgetsEnabledLandscape else settings.idleWidgetsEnabled
    val offAfterTap = if (landscape) settings.idleOffAfterTapCloseLandscape else settings.idleOffAfterTapClose
    val offAfterLongPress = if (landscape) settings.idleOffAfterLongPressLandscape else settings.idleOffAfterLongPress
    val selected = remember(settings.idleWidgetIds) { IdleWidgets.parse(settings.idleWidgetIds) }

    // The mode widget can only tell Bedtime / Driving / ... apart with Do Not Disturb access; the answer is read again when the user comes back from the system page.
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var dndAccess by remember { mutableStateOf(DndModeReader.hasPolicyAccess(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) dndAccess = DndModeReader.hasPolicyAccess(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .searchAnchor("idle.screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OrientationTabs(selected = tab, onSelected = { tab = it })

        // ---- Switches (per orientation) ----
        SectionCard {
            Column(modifier = Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SwitchRow(
                    title = stringResource(R.string.idle_widgets_enable),
                    subtitle = stringResource(R.string.idle_widgets_desc),
                    checked = enabled,
                    modifier = Modifier.searchAnchor("idle.enable"),
                    onCheckedChange = {
                        scope.launch {
                            if (landscape) repository.setIdleWidgetsEnabledLandscape(it) else repository.setIdleWidgetsEnabled(it)
                        }
                    }
                )
                if (enabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SwitchRow(
                        title = stringResource(R.string.idle_off_after_tap_title),
                        subtitle = stringResource(R.string.idle_off_after_tap_desc),
                        checked = offAfterTap,
                        modifier = Modifier.searchAnchor("idle.off_tap"),
                        onCheckedChange = { scope.launch { repository.setIdleOffAfterTapClose(landscape, it) } }
                    )
                    SwitchRow(
                        title = stringResource(R.string.idle_off_after_long_press_title),
                        subtitle = stringResource(R.string.idle_off_after_long_press_desc),
                        checked = offAfterLongPress,
                        modifier = Modifier.searchAnchor("idle.off_long_press"),
                        onCheckedChange = { scope.launch { repository.setIdleOffAfterLongPress(landscape, it) } }
                    )
                }
                Text(
                    text = stringResource(
                        if (landscape) R.string.idle_orientation_note_landscape else R.string.idle_orientation_note_portrait
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- Widget selector ----
        SectionCard(modifier = Modifier.searchAnchor("idle.select")) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.idle_select_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.idle_select_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    CountBadge("${selected.size} / ${IdleWidgetKind.values().size}")
                }
                // What is selected now, in the order the widgets take turns.
                selected.forEach { kind ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KindIcon(kind, checked = true)
                        Text(
                            text = stringResource(kindTitle(kind)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                FilledTonalButton(
                    onClick = { showSelector = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.idle_select_button), fontWeight = FontWeight.SemiBold)
                }
                if (IdleWidgetKind.Modes in selected && !dndAccess) {
                    Text(
                        text = stringResource(R.string.idle_modes_access_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.idle_modes_access_button), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ---- Timers (shared by portrait and landscape) ----
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.idle_timers_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TimerSlider(
                    title = stringResource(R.string.idle_tick_timer_title),
                    hint = stringResource(R.string.idle_tick_timer_desc),
                    stored = settings.idleGlyphTickSeconds,
                    range = IdleSlots.MIN_TICK_SECONDS..IdleSlots.MAX_TICK_SECONDS,
                    steps = Math.round((IdleSlots.MAX_TICK_SECONDS - IdleSlots.MIN_TICK_SECONDS) * 2f) - 1,
                    modifier = Modifier.searchAnchor("idle.tick_timer"),
                    label = { stringResource(R.string.idle_seconds_decimal, it) },
                    onCommit = { scope.launch { repository.setIdleGlyphTickSeconds(it) } }
                )
                TimerSlider(
                    title = stringResource(R.string.idle_switch_timer_title),
                    hint = stringResource(R.string.idle_switch_timer_desc),
                    stored = settings.idleWidgetCycleSeconds.toFloat(),
                    range = IdleWidgets.MIN_CYCLE_SECONDS.toFloat()..IdleWidgets.MAX_CYCLE_SECONDS.toFloat(),
                    steps = IdleWidgets.MAX_CYCLE_SECONDS - IdleWidgets.MIN_CYCLE_SECONDS - 1,
                    modifier = Modifier.searchAnchor("idle.switch_timer"),
                    label = { stringResource(R.string.idle_widgets_seconds, Math.round(it)) },
                    onCommit = { scope.launch { repository.setIdleWidgetCycleSeconds(IdleWidgetOff.normalizeCycleSeconds(Math.round(it))) } }
                )
                Text(
                    text = stringResource(R.string.idle_timers_shared_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showSelector) {
        WidgetSelectorDialog(
            storedIds = settings.idleWidgetIds,
            onChange = { ids -> scope.launch { repository.setIdleWidgetIds(ids) } },
            onDismiss = { showSelector = false }
        )
    }
}

/** The modal selector: a checkbox row per widget (icon, name, what it shows), Select all and Clear on top, like the pinned apps dialog. */
@Composable
private fun WidgetSelectorDialog(
    storedIds: String,
    onChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val selected = IdleWidgets.parse(storedIds)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Widgets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.idle_select_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                CountBadge("${selected.size} / ${IdleWidgetKind.values().size}")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onChange(IdleWidgetOff.allIds()) }) {
                        Text(stringResource(R.string.idle_select_all), fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { onChange(IdleWidgets.DEFAULT_IDS) }) {
                        Text(stringResource(R.string.idle_select_reset), fontWeight = FontWeight.SemiBold)
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(IdleWidgetKind.values().toList(), key = { it.id }) { kind ->
                        val checked = kind in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onChange(IdleWidgets.toggle(storedIds, kind)) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KindIcon(kind, checked)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(kindTitle(kind)),
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(kindDesc(kind)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // The last widget stays ticked: the master switch is what turns the widgets off.
                            Checkbox(
                                checked = checked,
                                enabled = !(checked && selected.size == 1),
                                onCheckedChange = { onChange(IdleWidgets.toggle(storedIds, kind)) }
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.idle_select_last_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.idle_select_done), fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
private fun KindIcon(kind: IdleWidgetKind, checked: Boolean) {
    val tint = kindTint(kind)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (checked) tint.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = kindIcon(kind),
            contentDescription = null,
            tint = if (checked) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CountBadge(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** A slider that follows the finger on its own copy and writes the setting once, when the finger lifts. */
@Composable
private fun TimerSlider(
    title: String,
    hint: String,
    stored: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
    label: @Composable (Float) -> String,
    onCommit: (Float) -> Unit
) {
    var value by remember(stored) { mutableFloatStateOf(stored) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label(value),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onCommit(value) },
            valueRange = range,
            steps = steps
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
