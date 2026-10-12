/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.model.PrivacyKind
import com.agupta07505.smartisland.util.PrivacyAppsInUse
import com.agupta07505.smartisland.util.openPrivacyUsage
import com.agupta07505.smartisland.util.runCatchingLogged
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * The small dialog a tapped privacy indicator (camera, microphone, location, screen share) opens: the apps using that
 * sensor right now. It replaces opening the system's settings screens, which the system refuses to open from this app.
 * Translucent: a tap outside closes it. The system's privacy dashboard stays one button away.
 */
class PrivacyUsageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val kind = PrivacyKind.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_KIND) }
        if (kind == null) { finish(); return }
        setContent {
            SmartIslandTheme(themeMode = "system") {
                PrivacyUsageDialog(kind = kind, onClose = { finish() }, onMore = { openPrivacyUsage(kind); finish() })
            }
        }
    }

    companion object {
        private const val EXTRA_KIND = "kind"
        fun intentFor(context: Context, kind: PrivacyKind): Intent =
            Intent(context, PrivacyUsageActivity::class.java)
                .putExtra(EXTRA_KIND, kind.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }
}

/** Opens the dialog for [kind]; false when it could not be started. */
fun Context.showPrivacyUsage(kind: PrivacyKind): Boolean =
    runCatchingLogged("PrivacyUsage", "Privacy usage dialog failed") {
        startActivity(PrivacyUsageActivity.intentFor(this, kind)); true
    } ?: openPrivacyUsage(kind)

@Composable
private fun PrivacyUsageDialog(kind: PrivacyKind, onClose: () -> Unit, onMore: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // null = loading, then the list (maybe empty) or "cannot read" (unreadable = true).
    var apps by remember { mutableStateOf<List<String>?>(null) }
    var unreadable by remember { mutableStateOf(false) }
    LaunchedEffect(kind) {
        val result = PrivacyAppsInUse.read(kind)
        unreadable = result == null
        apps = result.orEmpty().map { appLabel(context, it) }
    }
    Box(
        modifier = Modifier.fillMaxSize().clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose
        ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.padding(24.dp).fillMaxWidth().clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}
            )
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(titleRes(kind)), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                val list = apps
                when {
                    list == null -> Text(stringResource(R.string.privacy_usage_loading), fontSize = 14.sp)
                    unreadable -> Text(stringResource(R.string.privacy_usage_needs_shizuku), fontSize = 14.sp)
                    list.isEmpty() -> Text(stringResource(R.string.privacy_usage_none_found), fontSize = 14.sp)
                    else -> list.forEach { Text("\u2022 $it", fontSize = 15.sp) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onMore) { Text(stringResource(R.string.privacy_usage_more)) }
                    TextButton(onClick = onClose) { Text(stringResource(R.string.privacy_usage_close)) }
                }
            }
        }
    }
}

private fun titleRes(kind: PrivacyKind): Int = when (kind) {
    PrivacyKind.Camera -> R.string.privacy_usage_title_camera
    PrivacyKind.Microphone -> R.string.privacy_usage_title_microphone
    PrivacyKind.Location -> R.string.privacy_usage_title_location
    PrivacyKind.ScreenShare -> R.string.privacy_usage_title_screen
}

/** The app's name, or its package name when it cannot be looked up. */
private fun appLabel(context: Context, packageName: String): String = runCatching {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
}.getOrDefault(packageName)
