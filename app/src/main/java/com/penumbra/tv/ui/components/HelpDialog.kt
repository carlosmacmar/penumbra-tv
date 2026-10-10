package com.penumbra.tv.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import com.penumbra.tv.R
import com.penumbra.tv.ui.theme.LocalAccent
import com.penumbra.tv.ui.theme.Palette
import com.penumbra.tv.ui.theme.Type

/** Permissions that Google TV often hides from its settings; ADB is the reliable way to grant them. */
enum class HelpTopic(@StringRes val title: Int, @StringRes val body: Int, private val appOp: String) {
    Overlay(R.string.help_overlay_title, R.string.help_overlay_body, "SYSTEM_ALERT_WINDOW"),
    Brightness(R.string.help_brightness_title, R.string.help_brightness_body, "WRITE_SETTINGS");

    fun adbCommand(packageName: String) = "adb shell appops set $packageName $appOp allow"
}

@Composable
fun HelpDialog(
    topic: HelpTopic,
    packageName: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val firstButton = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(600.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Palette.Surface)
                .border(1.dp, Palette.Hairline, RoundedCornerShape(24.dp))
                .padding(36.dp),
        ) {
            Text(stringResource(topic.title), style = Type.StatusTitle.copy(fontSize = 24.sp))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(topic.body), style = Type.Body, color = Palette.TextSecondary)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.Background)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Text(
                    text = topic.adbCommand(packageName),
                    style = Type.Body.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                    color = LocalAccent.current,
                )
            }
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PillButton(
                    text = stringResource(R.string.help_open_settings),
                    onClick = onOpenSettings,
                    primary = true,
                    modifier = Modifier.focusRequester(firstButton),
                )
                PillButton(text = stringResource(R.string.help_close), onClick = onDismiss)
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { firstButton.requestFocus() } }
}
