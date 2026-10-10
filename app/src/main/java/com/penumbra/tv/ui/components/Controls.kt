package com.penumbra.tv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.penumbra.tv.R
import com.penumbra.tv.core.Tone
import com.penumbra.tv.ui.theme.LocalAccent
import com.penumbra.tv.ui.theme.Palette
import com.penumbra.tv.ui.theme.Type

private val RowShape = RoundedCornerShape(16.dp)

/**
 * Base for every settings row: one focusable surface per row. Up/down moves between rows,
 * OK activates, and — when [onStep] is set — left/right change the value in place.
 */
@Composable
fun SettingSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onStep: ((delta: Int, repeat: Int) -> Unit)? = null,
    content: @Composable (focused: Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val stepKeys = if (onStep == null) {
        Modifier
    } else {
        Modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val delta = when (event.key) {
                Key.DirectionLeft -> -1
                Key.DirectionRight -> 1
                else -> return@onPreviewKeyEvent false
            }
            onStep(delta, event.nativeKeyEvent.repeatCount)
            true
        }
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(stepKeys),
        shape = ClickableSurfaceDefaults.shape(RowShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Palette.TextPrimary,
            focusedContainerColor = Palette.SurfaceFocused,
            focusedContentColor = Palette.TextPrimary,
            pressedContainerColor = Palette.SurfaceFocused,
            pressedContentColor = Palette.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.015f),
        interactionSource = interaction,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp),
        ) {
            content(focused)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = Type.Section,
        color = Palette.TextTertiary,
        modifier = modifier.padding(start = 22.dp, top = 30.dp, bottom = 8.dp),
    )
}

@Composable
private fun TitleBlock(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = Type.RowTitle)
        if (subtitle != null) {
            Spacer(Modifier.height(3.dp))
            Text(subtitle, style = Type.RowSubtitle, color = Palette.TextSecondary)
        }
    }
}

/** "‹ value ›" — the arrows appear while the row is focused, as a hint that left/right work. */
@Composable
private fun StepperValue(value: String, focused: Boolean, minWidth: Dp = 0.dp) {
    val accent = LocalAccent.current
    val color by animateColorAsState(if (focused) accent else Palette.TextSecondary, label = "value")
    val arrows by animateFloatAsState(if (focused) 1f else 0f, label = "arrows")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Chevron(pointsLeft = true, color = color, modifier = Modifier.size(7.dp, 12.dp).alpha(arrows))
        Text(
            text = value,
            style = Type.Value,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .widthIn(min = minWidth),
        )
        Chevron(pointsLeft = false, color = color, modifier = Modifier.size(7.dp, 12.dp).alpha(arrows))
    }
}

@Composable
fun SliderRow(
    title: String,
    percent: Int,
    onStep: (delta: Int, repeat: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingSurface(onClick = {}, modifier = modifier, onStep = onStep) { focused ->
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = Type.RowTitle, modifier = Modifier.weight(1f))
                StepperValue(stringResource(R.string.percent, percent), focused, minWidth = 56.dp)
            }
            Spacer(Modifier.height(14.dp))
            LevelBar(percent / 100f, focused)
        }
    }
}

@Composable
private fun LevelBar(fraction: Float, focused: Boolean) {
    val accent = LocalAccent.current
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "level",
    )
    val fill by animateColorAsState(if (focused) accent else accent.copy(alpha = 0.5f), label = "fill")
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(Palette.Track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(fill),
        )
    }
}

@Composable
fun StepperRow(
    title: String,
    value: String,
    onStep: (delta: Int, repeat: Int) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: () -> Unit = { onStep(1, 0) },
) {
    SettingSurface(onClick = onClick, modifier = modifier, onStep = onStep) { focused ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            TitleBlock(title, subtitle, Modifier.weight(1f))
            Spacer(Modifier.width(20.dp))
            StepperValue(value, focused, minWidth = 64.dp)
        }
    }
}

@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    SettingSurface(onClick = { onCheckedChange(!checked) }, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TitleBlock(title, subtitle, Modifier.weight(1f))
            Spacer(Modifier.width(20.dp))
            PillSwitch(checked)
        }
    }
}

@Composable
fun InfoRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    healthy: Boolean = true,
) {
    SettingSurface(onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TitleBlock(title, subtitle, Modifier.weight(1f))
            Spacer(Modifier.width(20.dp))
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (healthy) LocalAccent.current else Color(0xFFE5484D)),
            )
            Spacer(Modifier.width(10.dp))
            Text(value, style = Type.Value, color = Palette.TextSecondary)
        }
    }
}

@Composable
fun ToneRow(
    title: String,
    selected: Tone,
    onStep: (delta: Int, repeat: Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingSurface(onClick = onClick, modifier = modifier, onStep = onStep) { focused ->
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = Type.RowTitle, modifier = Modifier.weight(1f))
                StepperValue(stringResource(selected.label), focused, minWidth = 64.dp)
            }
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tone.entries.forEach { tone -> ToneSwatch(tone, tone == selected) }
            }
        }
    }
}

@Composable
private fun ToneSwatch(tone: Tone, selected: Boolean) {
    val color = Color(tone.rgb)
    val dot by animateDpAsState(if (selected) 20.dp else 12.dp, label = "swatch")
    val alpha by animateFloatAsState(if (selected) 1f else 0.7f, label = "swatchAlpha")
    val ring by animateFloatAsState(if (selected) 1f else 0f, label = "swatchRing")
    Box(
        Modifier
            .size(30.dp)
            .drawBehind {
                drawCircle(
                    color = color.copy(alpha = 0.55f * ring),
                    radius = size.minDimension / 2 - 1.dp.toPx(),
                    style = Stroke(1.5.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(dot)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha)),
        )
    }
}

@Composable
fun PillSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val track by animateColorAsState(if (checked) accent else Palette.Track, label = "track")
    val knob by animateColorAsState(if (checked) Palette.Background else Palette.TextSecondary, label = "knob")
    val offset by animateDpAsState(if (checked) 18.dp else 0.dp, label = "knobOffset")
    Box(
        modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(CircleShape)
            .background(track)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset { IntOffset(offset.roundToPx(), 0) }
                .size(20.dp)
                .clip(CircleShape)
                .background(knob),
        )
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val accent = LocalAccent.current
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (primary) accent.copy(alpha = 0.16f) else Palette.Track,
            contentColor = if (primary) accent else Palette.TextPrimary,
            focusedContainerColor = if (primary) accent else Palette.TextPrimary,
            focusedContentColor = Palette.Background,
            pressedContainerColor = if (primary) accent else Palette.TextPrimary,
            pressedContentColor = Palette.Background,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
    ) {
        Text(text, style = Type.Button, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
    }
}

private val PowerButtonSize = 128.dp

/** The hero control: a circle that fills with the tone colour and glows while the filter is on. */
@Composable
fun PowerButton(
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val container by animateColorAsState(
        targetValue = when {
            on -> accent
            focused -> Palette.SurfaceFocused
            else -> Palette.Surface
        },
        animationSpec = tween(500),
        label = "powerContainer",
    )
    val icon by animateColorAsState(
        targetValue = when {
            on -> Palette.Background
            focused -> Palette.TextPrimary
            else -> Palette.TextSecondary
        },
        animationSpec = tween(500),
        label = "powerIcon",
    )
    val ring by animateFloatAsState(if (focused) 1f else 0f, tween(250), label = "powerRing")
    val halo by animateFloatAsState(if (on) 1f else 0f, tween(900), label = "powerHalo")
    val description = stringResource(if (on) R.string.power_off_description else R.string.power_on_description)

    Box(
        Modifier
            .size(PowerButtonSize + 30.dp)
            .drawBehind {
                val haloRadius = size.minDimension * 1.05f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.42f * halo), Color.Transparent),
                        center = center,
                        radius = haloRadius,
                    ),
                    radius = haloRadius,
                )
                drawCircle(
                    color = (if (on) accent else Palette.TextPrimary).copy(alpha = 0.75f * ring),
                    radius = size.minDimension / 2 - 1.dp.toPx(),
                    style = Stroke(2.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            modifier = modifier
                .size(PowerButtonSize)
                .semantics { contentDescription = description },
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = container,
                contentColor = icon,
                focusedContainerColor = container,
                focusedContentColor = icon,
                pressedContainerColor = container,
                pressedContentColor = icon,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 0.96f),
            interactionSource = interaction,
        ) {
            Box(
                Modifier
                    .size(PowerButtonSize)
                    .border(1.dp, if (on) Color.Transparent else Palette.Hairline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                PowerIcon(icon, Modifier.size(46.dp))
            }
        }
    }
}
