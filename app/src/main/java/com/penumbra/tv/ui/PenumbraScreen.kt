package com.penumbra.tv.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.penumbra.tv.R
import com.penumbra.tv.core.FilterState
import com.penumbra.tv.core.FilterStore
import com.penumbra.tv.core.PenumbraController
import com.penumbra.tv.core.Schedule
import com.penumbra.tv.core.SystemBrightness
import com.penumbra.tv.core.Tone
import com.penumbra.tv.overlay.OverlayAccessibilityService
import com.penumbra.tv.overlay.OverlayHost
import com.penumbra.tv.ui.components.HelpDialog
import com.penumbra.tv.ui.components.HelpTopic
import com.penumbra.tv.ui.components.InfoRow
import com.penumbra.tv.ui.components.MoonIcon
import com.penumbra.tv.ui.components.PillButton
import com.penumbra.tv.ui.components.PowerButton
import com.penumbra.tv.ui.components.SectionLabel
import com.penumbra.tv.ui.components.SliderRow
import com.penumbra.tv.ui.components.StepperRow
import com.penumbra.tv.ui.components.ToggleRow
import com.penumbra.tv.ui.components.ToneRow
import com.penumbra.tv.ui.theme.LocalAccent
import com.penumbra.tv.ui.theme.Palette
import com.penumbra.tv.ui.theme.PenumbraTheme
import com.penumbra.tv.ui.theme.Type
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay

/** TV-safe margins: 5 % of a 960 × 540 dp screen. */
private val SafeHorizontal = 48.dp
private val SafeVertical = 36.dp

@Composable
fun PenumbraScreen() {
    val context = LocalContext.current
    val state by FilterStore.state.collectAsStateWithLifecycle()
    val accessibilityOn by OverlayAccessibilityService.connected.collectAsStateWithLifecycle()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    // Re-checked on every resume, since permissions are granted outside the app.
    val mode = remember(accessibilityOn, lifecycle) { OverlayHost.mode(context) }
    val canWriteSettings = remember(lifecycle) { SystemBrightness.canWrite(context) }
    val now by rememberNow()

    var help by remember { mutableStateOf<HelpTopic?>(null) }
    var powerFocused by remember { mutableStateOf(false) }
    val powerFocus = remember { FocusRequester() }
    val setupFocus = remember { FocusRequester() }

    val accent by animateColorAsState(Color(state.tone.rgb), tween(700), label = "accent")
    // Without a permission nothing is drawn, so the filter must not look active.
    val active = state.enabled && mode != OverlayHost.Mode.NONE
    val ambient by animateFloatAsState(if (active) 0.14f else 0.05f, tween(1200), label = "ambient")

    PenumbraTheme(accent) {
        Row(
            Modifier
                .fillMaxSize()
                .background(Palette.Background)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = ambient), Color.Transparent),
                            center = Offset(size.width * 0.14f, size.height * 0.78f),
                            radius = size.width * 0.55f,
                        ),
                    )
                }
                .padding(horizontal = SafeHorizontal),
        ) {
            StatusPane(
                state = state,
                now = now,
                mode = mode,
                onToggle = {
                    if (mode == OverlayHost.Mode.NONE) {
                        runCatching { setupFocus.requestFocus() }
                    } else {
                        PenumbraController.toggle(context)
                    }
                },
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxHeight(),
                powerModifier = Modifier
                    .focusRequester(powerFocus)
                    .onFocusChanged { powerFocused = it.isFocused },
            )
            SettingsPane(
                state = state,
                mode = mode,
                canWriteSettings = canWriteSettings,
                setupFocus = setupFocus,
                onHelp = { help = it },
                modifier = Modifier
                    .weight(0.54f)
                    .fillMaxHeight(),
            )
        }

        help?.let { topic ->
            HelpDialog(
                topic = topic,
                packageName = context.packageName,
                onOpenSettings = {
                    val opened = when (topic) {
                        HelpTopic.Overlay -> SystemScreens.openOverlayPermission(context)
                        HelpTopic.Brightness -> SystemScreens.openWriteSettings(context)
                    }
                    if (opened) {
                        help = null
                    } else {
                        Toast.makeText(context, R.string.help_settings_unavailable, Toast.LENGTH_LONG).show()
                    }
                },
                onDismiss = { help = null },
            )
        }
    }

    // Start on the setup card while a permission is missing, otherwise on the power button.
    LaunchedEffect(mode) {
        runCatching {
            if (mode == OverlayHost.Mode.NONE) setupFocus.requestFocus() else powerFocus.requestFocus()
        }
    }
    // Back returns to the power button first; a second press leaves the app.
    BackHandler(enabled = !powerFocused) { runCatching { powerFocus.requestFocus() } }
}

@Composable
private fun StatusPane(
    state: FilterState,
    now: Long,
    mode: OverlayHost.Mode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    powerModifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val active = state.enabled && mode != OverlayHost.Mode.NONE
    Column(
        modifier.padding(top = SafeVertical + 8.dp, bottom = SafeVertical + 8.dp, end = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoonIcon(LocalAccent.current, Modifier.size(15.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.app_name).uppercase(), style = Type.Brand, color = LocalAccent.current)
            }
            Spacer(Modifier.height(30.dp))
            val (clock, dayPeriod) = formatClock(context, now)
            Row {
                Text(clock, style = Type.Clock, modifier = Modifier.alignByBaseline())
                if (dayPeriod != null) {
                    Spacer(Modifier.width(14.dp))
                    Text(
                        dayPeriod,
                        style = Type.StatusTitle,
                        color = Palette.TextSecondary,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
            Text(formatDate(now), style = Type.Date, color = Palette.TextSecondary)
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PowerButton(on = active, onClick = onToggle, modifier = powerModifier)
                Spacer(Modifier.width(26.dp))
                Column {
                    Text(
                        stringResource(if (active) R.string.status_on else R.string.status_off),
                        style = Type.StatusTitle,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(statusDetail(context, state, now, mode), style = Type.Body, color = Palette.TextSecondary)
                }
            }
            Spacer(Modifier.height(26.dp))
            NextEvent(state, now)
        }
    }
}

private fun statusDetail(context: Context, state: FilterState, now: Long, mode: OverlayHost.Mode): String = when {
    mode == OverlayHost.Mode.NONE -> context.getString(R.string.status_hint_setup)
    !state.enabled -> context.getString(R.string.status_hint_off)
    state.isRamping(now) -> context.getString(R.string.status_ramping)
    state.tone.tints -> context.getString(R.string.status_dim_tone, state.dim, context.getString(state.tone.label))
    else -> context.getString(R.string.status_dim, state.dim)
}

@Composable
private fun NextEvent(state: FilterState, now: Long) {
    val context = LocalContext.current
    val next = if (state.scheduleEnabled) Schedule.next(state, now) else null
    Row(Modifier.height(24.dp), verticalAlignment = Alignment.CenterVertically) {
        if (next == null) return@Row
        val zone = ZoneId.systemDefault()
        val at = Instant.ofEpochMilli(next.at).atZone(zone)
        val today = at.toLocalDate() == Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val time = formatTime(context, at.toLocalTime())
        val text = when {
            next.turnsOn && today -> stringResource(R.string.next_on_today, time)
            next.turnsOn -> stringResource(R.string.next_on_tomorrow, time)
            today -> stringResource(R.string.next_off_today, time)
            else -> stringResource(R.string.next_off_tomorrow, time)
        }
        Spacer(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(LocalAccent.current),
        )
        Spacer(Modifier.width(12.dp))
        Text(text, style = Type.Body, color = Palette.TextSecondary)
    }
}

@Composable
private fun SettingsPane(
    state: FilterState,
    mode: OverlayHost.Mode,
    canWriteSettings: Boolean,
    setupFocus: FocusRequester,
    onHelp: (HelpTopic) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier
            .fadingEdges(SafeVertical)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = SafeVertical),
    ) {
        if (mode == OverlayHost.Mode.NONE) {
            SetupCard(
                focusRequester = setupFocus,
                onOpenAccessibility = { SystemScreens.openAccessibility(context) },
                onOtherOptions = { onHelp(HelpTopic.Overlay) },
            )
        }

        SectionLabel(stringResource(R.string.section_screen))
        SliderRow(
            title = stringResource(R.string.setting_dim),
            percent = state.dim,
            onStep = { delta, _ ->
                PenumbraController.update(context) { it.copy(dim = (it.dim + delta * 5).coerceIn(0, 100)) }
            },
        )
        ToneRow(
            title = stringResource(R.string.setting_tone),
            selected = state.tone,
            onStep = { delta, _ ->
                PenumbraController.update(context) {
                    it.copy(tone = Tone.entries[(it.tone.ordinal + delta).coerceIn(0, Tone.entries.lastIndex)])
                }
            },
            onClick = {
                PenumbraController.update(context) {
                    it.copy(tone = Tone.entries[(it.tone.ordinal + 1) % Tone.entries.size])
                }
            },
        )
        AnimatedVisibility(visible = state.tone.tints) {
            SliderRow(
                title = stringResource(R.string.setting_warmth),
                percent = state.warmth,
                onStep = { delta, _ ->
                    PenumbraController.update(context) { it.copy(warmth = (it.warmth + delta * 5).coerceIn(0, 100)) }
                },
            )
        }

        SectionLabel(stringResource(R.string.section_schedule))
        ToggleRow(
            title = stringResource(R.string.setting_schedule),
            subtitle = stringResource(R.string.setting_schedule_hint),
            checked = state.scheduleEnabled,
            onCheckedChange = { enabled ->
                PenumbraController.updateSchedule(context) { it.copy(scheduleEnabled = enabled) }
            },
        )
        AnimatedVisibility(visible = state.scheduleEnabled) {
            Column {
                StepperRow(
                    title = stringResource(R.string.setting_start),
                    value = formatMinuteOfDay(context, state.startMinute),
                    onStep = { delta, repeat ->
                        PenumbraController.updateSchedule(context) {
                            it.copy(startMinute = shiftMinute(it.startMinute, delta, repeat))
                        }
                    },
                )
                StepperRow(
                    title = stringResource(R.string.setting_end),
                    value = formatMinuteOfDay(context, state.endMinute),
                    onStep = { delta, repeat ->
                        PenumbraController.updateSchedule(context) {
                            it.copy(endMinute = shiftMinute(it.endMinute, delta, repeat))
                        }
                    },
                )
                StepperRow(
                    title = stringResource(R.string.setting_fade),
                    subtitle = stringResource(R.string.setting_fade_hint),
                    value = formatFade(context, state.fadeMinutes),
                    onStep = { delta, _ ->
                        PenumbraController.update(context) { it.copy(fadeMinutes = shiftFade(it.fadeMinutes, delta)) }
                    },
                )
            }
        }

        SectionLabel(stringResource(R.string.section_system))
        ToggleRow(
            title = stringResource(R.string.setting_wake),
            subtitle = stringResource(R.string.setting_wake_hint),
            checked = state.activateOnWake,
            onCheckedChange = { enabled -> PenumbraController.update(context) { it.copy(activateOnWake = enabled) } },
        )
        ToggleRow(
            title = stringResource(R.string.setting_brightness),
            subtitle = stringResource(
                if (state.systemBrightness && !canWriteSettings) {
                    R.string.setting_brightness_missing
                } else {
                    R.string.setting_brightness_hint
                },
            ),
            checked = state.systemBrightness,
            onCheckedChange = { enabled ->
                PenumbraController.update(context) { it.copy(systemBrightness = enabled) }
                if (enabled && !canWriteSettings) onHelp(HelpTopic.Brightness)
            },
        )
        InfoRow(
            title = stringResource(R.string.setting_mode),
            subtitle = stringResource(R.string.setting_mode_hint),
            value = stringResource(
                when (mode) {
                    OverlayHost.Mode.ACCESSIBILITY -> R.string.mode_accessibility
                    OverlayHost.Mode.OVERLAY -> R.string.mode_overlay
                    OverlayHost.Mode.NONE -> R.string.mode_none
                },
            ),
            healthy = mode != OverlayHost.Mode.NONE,
            onClick = { SystemScreens.openAccessibility(context) },
        )
    }
}

@Composable
private fun SetupCard(
    focusRequester: FocusRequester,
    onOpenAccessibility: () -> Unit,
    onOtherOptions: () -> Unit,
) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(shape)
            .background(Palette.Surface)
            .border(1.dp, accent.copy(alpha = 0.3f), shape)
            .padding(26.dp),
    ) {
        Text(stringResource(R.string.setup_title), style = Type.RowTitle.copy(fontSize = 20.sp))
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.setup_body), style = Type.Body, color = Palette.TextSecondary)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(
                text = stringResource(R.string.setup_open_accessibility),
                onClick = onOpenAccessibility,
                primary = true,
                modifier = Modifier.focusRequester(focusRequester),
            )
            PillButton(text = stringResource(R.string.setup_other), onClick = onOtherOptions)
        }
    }
}

/** Softly fades the scrolling list into the background at its top and bottom edges. */
private fun Modifier.fadingEdges(edge: Dp): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val stop = (edge.toPx() / size.height).coerceIn(0f, 0.5f)
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                stop to Color.Black,
                1f - stop to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

@Composable
private fun rememberNow(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        delay(1_000L - System.currentTimeMillis() % 1_000L)
        value = System.currentTimeMillis()
    }
}
