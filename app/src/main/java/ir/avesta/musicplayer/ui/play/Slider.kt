package ir.avesta.musicplayer.ui.play

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.contentColorFor
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbRadius: Dp = 8.dp,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    /*@IntRange(from = 0)*/
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SliderColors = SliderDefaults.colors()
) {
    require(steps >= 0) { "steps should be >= 0" }
    val onValueChangeState = rememberUpdatedState(onValueChange)
    val tickFractions = remember(steps) {
        stepsToTickFractions(steps)
    }
    BoxWithConstraints(
        modifier
            .focusable(enabled, interactionSource)
    ) {
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val maxPx = constraints.maxWidth.toFloat()
        val minPx = 0f

        fun scaleToUserValue(offset: Float) =
            scale(minPx, maxPx, offset, valueRange.start, valueRange.endInclusive)

        fun scaleToOffset(userValue: Float) =
            scale(valueRange.start, valueRange.endInclusive, userValue, minPx, maxPx)

        val scope = rememberCoroutineScope()
        val rawOffset = remember { mutableStateOf(scaleToOffset(value)) }
        val draggableState = remember(minPx, maxPx, valueRange) {
            SliderDraggableState {
                rawOffset.value = (rawOffset.value + it).coerceIn(minPx, maxPx)
                onValueChangeState.value.invoke(scaleToUserValue(rawOffset.value))
            }
        }

        CorrectValueSideEffect(::scaleToOffset, valueRange, rawOffset, value)

        val gestureEndAction = rememberUpdatedState<(Float) -> Unit> { velocity: Float ->
            val current = rawOffset.value
            val target = snapValueToTick(current, tickFractions, minPx, maxPx)
            if (current != target) {
                scope.launch {
                    animateToTarget(draggableState, current, target, velocity)
                    onValueChangeFinished?.invoke()
                }
            } else if (!draggableState.isDragging) {
                // check ifDragging in case the change is still in progress (touch -> drag case)
                onValueChangeFinished?.invoke()
            }
        }

        val press = Modifier.sliderPressModifier(
            draggableState,
            interactionSource,
            maxPx,
            isRtl,
            rawOffset,
            gestureEndAction,
            enabled
        )

        val drag = Modifier.draggable(
            orientation = Orientation.Horizontal,
            reverseDirection = isRtl,
            enabled = enabled,
            interactionSource = interactionSource,
            onDragStopped = { velocity -> gestureEndAction.value.invoke(velocity) },
            startDragImmediately = draggableState.isDragging,
            state = draggableState
        )

        val coerced = value.coerceIn(valueRange.start, valueRange.endInclusive)
        val fraction = calcFraction(valueRange.start, valueRange.endInclusive, coerced)
        SliderImpl(
            enabled,
            fraction,
            tickFractions,
            colors,
            maxPx,
            thumbRadius,
            interactionSource,
            modifier = press.then(drag)
        )
    }
}

object SliderDefaults {

    @Composable
    fun colors(
        thumbColor: Color = MaterialTheme.colors.primary,
        disabledThumbColor: Color = MaterialTheme.colors.onSurface
            .copy(alpha = ContentAlpha.disabled)
            .compositeOver(MaterialTheme.colors.surface),
        activeTrackColor: Color = MaterialTheme.colors.primary,
        inactiveTrackColor: Color = activeTrackColor.copy(alpha = InactiveTrackAlpha),
        disabledActiveTrackColor: Color =
            MaterialTheme.colors.onSurface.copy(alpha = DisabledActiveTrackAlpha),
        disabledInactiveTrackColor: Color =
            disabledActiveTrackColor.copy(alpha = DisabledInactiveTrackAlpha),
        activeTickColor: Color = contentColorFor(activeTrackColor).copy(alpha = TickAlpha),
        inactiveTickColor: Color = activeTrackColor.copy(alpha = TickAlpha),
        disabledActiveTickColor: Color = activeTickColor.copy(alpha = DisabledTickAlpha),
        disabledInactiveTickColor: Color = disabledInactiveTrackColor
            .copy(alpha = DisabledTickAlpha)
    ): SliderColors = DefaultSliderColors(
        thumbColor = thumbColor,
        disabledThumbColor = disabledThumbColor,
        activeTrackColor = activeTrackColor,
        inactiveTrackColor = inactiveTrackColor,
        disabledActiveTrackColor = disabledActiveTrackColor,
        disabledInactiveTrackColor = disabledInactiveTrackColor,
        activeTickColor = activeTickColor,
        inactiveTickColor = inactiveTickColor,
        disabledActiveTickColor = disabledActiveTickColor,
        disabledInactiveTickColor = disabledInactiveTickColor
    )

    const val InactiveTrackAlpha = 0.24f

    const val DisabledInactiveTrackAlpha = 0.12f

    const val DisabledActiveTrackAlpha = 0.32f

    const val TickAlpha = 0.54f

    const val DisabledTickAlpha = 0.12f
}

@Stable
interface SliderColors {

    /**
     * Represents the color used for the sliders's thumb, depending on [enabled].
     *
     * @param enabled whether the [Slider] is enabled or not
     */
    @Composable
    fun thumbColor(enabled: Boolean): State<Color>

    /**
     * Represents the color used for the sliders's track, depending on [enabled] and [active].
     *
     * Active part is filled with progress, so if sliders progress is 30% out of 100%, left (or
     * right in RTL) 30% of the track will be active, the rest is not active.
     *
     * @param enabled whether the [Slider] is enabled or not
     * @param active whether the part of the track is active of not
     */
    @Composable
    fun trackColor(enabled: Boolean, active: Boolean): State<Color>

    /**
     * Represents the color used for the sliders's tick which is the dot separating steps, if
     * they are set on the slider, depending on [enabled] and [active].
     *
     * Active tick is the tick that is in the part of the track filled with progress, so if
     * sliders progress is 30% out of 100%, left (or right in RTL) 30% of the track and the ticks
     * in this 30% will be active, the rest is not active.
     *
     * @param enabled whether the [Slider] is enabled or not
     * @param active whether the part of the track this tick is in is active of not
     */
    @Composable
    fun tickColor(enabled: Boolean, active: Boolean): State<Color>
}

@Composable
private fun SliderImpl(
    enabled: Boolean,
    positionFraction: Float,
    tickFractions: List<Float>,
    colors: SliderColors,
    width: Float,
    thumbRadius: Dp,
    interactionSource: MutableInteractionSource,
    modifier: Modifier
) {
    Box(modifier.then(DefaultSliderConstraints)) {
        val trackStrokeWidth: Float
        val thumbPx: Float
        val widthDp: Dp
        with(LocalDensity.current) {
            trackStrokeWidth = TrackHeight.toPx()
            thumbPx = thumbRadius.toPx()
            widthDp = width.toDp()
        }

        val thumbSize = thumbRadius * 2
        val offset = (widthDp - thumbSize) * positionFraction
        val center = Modifier.align(Alignment.CenterStart)

        Track(
            center.fillMaxSize(),
            colors,
            enabled,
            0f,
            positionFraction,
            tickFractions,
            thumbPx,
            trackStrokeWidth
        )
        SliderThumb(center, offset, interactionSource, colors, enabled, thumbSize)
    }
}

@Composable
private fun SliderThumb(
    modifier: Modifier,
    offset: Dp,
    interactionSource: MutableInteractionSource,
    colors: SliderColors,
    enabled: Boolean,
    thumbSize: Dp
) {
    Box(modifier.padding(start = offset)) {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> interactions.add(interaction)
                    is PressInteraction.Release -> interactions.remove(interaction.press)
                    is PressInteraction.Cancel -> interactions.remove(interaction.press)
                    is DragInteraction.Start -> interactions.add(interaction)
                    is DragInteraction.Stop -> interactions.remove(interaction.start)
                    is DragInteraction.Cancel -> interactions.remove(interaction.start)
                }
            }
        }

        val elevation = if (interactions.isNotEmpty()) {
            ThumbPressedElevation
        } else {
            ThumbDefaultElevation
        }
        Spacer(
            Modifier
                .size(thumbSize, thumbSize)
                .indication(
                    interactionSource = interactionSource,
                    indication = rememberRipple(bounded = false, radius = ThumbRippleRadius)
                )
                .shadow(if (enabled) elevation else 0.dp, CircleShape, clip = false)
                .background(colors.thumbColor(enabled).value, CircleShape)
        )
    }
}

@Composable
private fun Track(
    modifier: Modifier,
    colors: SliderColors,
    enabled: Boolean,
    positionFractionStart: Float,
    positionFractionEnd: Float,
    tickFractions: List<Float>,
    thumbPx: Float,
    trackStrokeWidth: Float
) {
    val inactiveTrackColor = colors.trackColor(enabled, active = false)
    val activeTrackColor = colors.trackColor(enabled, active = true)
    val inactiveTickColor = colors.tickColor(enabled, active = false)
    val activeTickColor = colors.tickColor(enabled, active = true)
    Canvas(modifier) {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val sliderLeft = Offset(thumbPx, center.y)
        val sliderRight = Offset(size.width - thumbPx, center.y)
        val sliderStart = if (isRtl) sliderRight else sliderLeft
        val sliderEnd = if (isRtl) sliderLeft else sliderRight
        drawLine(
            inactiveTrackColor.value,
            sliderStart,
            sliderEnd,
            trackStrokeWidth,
            StrokeCap.Round
        )
        val sliderValueEnd = Offset(
            sliderStart.x + (sliderEnd.x - sliderStart.x) * positionFractionEnd,
            center.y
        )

        val sliderValueStart = Offset(
            sliderStart.x + (sliderEnd.x - sliderStart.x) * positionFractionStart,
            center.y
        )

        drawLine(
            activeTrackColor.value,
            sliderValueStart,
            sliderValueEnd,
            trackStrokeWidth,
            StrokeCap.Round
        )
        tickFractions.groupBy { it > positionFractionEnd }.forEach { (afterFraction, list) ->
            drawPoints(
                list.map {
                    Offset(lerp(sliderStart, sliderEnd, it).x, center.y)
                },
                PointMode.Points,
                (if (afterFraction) inactiveTickColor else activeTickColor).value,
                trackStrokeWidth,
                StrokeCap.Round
            )
        }
    }
}

private fun snapValueToTick(
    current: Float,
    tickFractions: List<Float>,
    minPx: Float,
    maxPx: Float
): Float {
    // target is a closest anchor to the `current`, if exists
    return tickFractions
        .minByOrNull { abs(lerp(minPx, maxPx, it) - current) }
        ?.run { lerp(minPx, maxPx, this) }
        ?: current
}

private suspend fun AwaitPointerEventScope.awaitSlop(
    id: PointerId
): Pair<PointerInputChange, Float>? {
    var initialDelta = 0f
    val postTouchSlop = { pointerInput: PointerInputChange, offset: Float ->
        pointerInput.consumePositionChange()
        initialDelta = offset
    }
    val afterSlopResult = awaitHorizontalTouchSlopOrCancellation(id, postTouchSlop)
    return if (afterSlopResult != null) afterSlopResult to initialDelta else null
}

private fun stepsToTickFractions(steps: Int): List<Float> {
    return if (steps == 0) emptyList() else List(steps + 2) { it.toFloat() / (steps + 1) }
}

private fun scale(a1: Float, b1: Float, x1: Float, a2: Float, b2: Float) =
    lerp(a2, b2, calcFraction(a1, b1, x1))

private fun scale(a1: Float, b1: Float, x: ClosedFloatingPointRange<Float>, a2: Float, b2: Float) =
    scale(a1, b1, x.start, a2, b2)..scale(a1, b1, x.endInclusive, a2, b2)

private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)

@Composable
private fun CorrectValueSideEffect(
    scaleToOffset: (Float) -> Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueState: MutableState<Float>,
    value: Float
) {
    SideEffect {
        val error = (valueRange.endInclusive - valueRange.start) / 1000
        val newOffset = scaleToOffset(value)
        if (abs(newOffset - valueState.value) > error)
            valueState.value = newOffset
    }
}

private fun Modifier.sliderSemantics(
    value: Float,
    tickFractions: List<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
): Modifier {
    val coerced = value.coerceIn(valueRange.start, valueRange.endInclusive)
    return semantics(mergeDescendants = true) {
        if (!enabled) disabled()
        setProgress(
            action = { targetValue ->
                val newValue = targetValue.coerceIn(valueRange.start, valueRange.endInclusive)
                val resolvedValue = if (steps > 0) {
                    tickFractions
                        .map { lerp(valueRange.start, valueRange.endInclusive, it) }
                        .minByOrNull { abs(it - newValue) } ?: newValue
                } else {
                    newValue
                }
                // This is to keep it consistent with AbsSeekbar.java: return false if no
                // change from current.
                if (resolvedValue == coerced) {
                    false
                } else {
                    onValueChange(resolvedValue)
                    true
                }
            }
        )
    }.progressSemantics(value, valueRange, steps)
}

private fun Modifier.sliderPressModifier(
    draggableState: DraggableState,
    interactionSource: MutableInteractionSource,
    maxPx: Float,
    isRtl: Boolean,
    rawOffset: State<Float>,
    gestureEndAction: State<(Float) -> Unit>,
    enabled: Boolean
): Modifier =
    if (enabled) {
        pointerInput(draggableState, interactionSource, maxPx, isRtl) {
            detectTapGestures(
                onPress = { pos ->
                    draggableState.drag(MutatePriority.UserInput) {
                        val to = if (isRtl) maxPx - pos.x else pos.x
                        dragBy(to - rawOffset.value)
                    }
                    val interaction = PressInteraction.Press(pos)
                    interactionSource.emit(interaction)
                    val finishInteraction =
                        try {
                            val success = tryAwaitRelease()
                            gestureEndAction.value.invoke(0f)
                            if (success) {
                                PressInteraction.Release(interaction)
                            } else {
                                PressInteraction.Cancel(interaction)
                            }
                        } catch (c: CancellationException) {
                            PressInteraction.Cancel(interaction)
                        }
                    interactionSource.emit(finishInteraction)
                }
            )
        }
    } else {
        this
    }

private suspend fun animateToTarget(
    draggableState: DraggableState,
    current: Float,
    target: Float,
    velocity: Float
) {
    draggableState.drag {
        var latestValue = current
        Animatable(initialValue = current).animateTo(target, SliderToTickAnimation, velocity) {
            dragBy(this.value - latestValue)
            latestValue = this.value
        }
    }
}

private fun Modifier.rangeSliderPressDragModifier(
    startInteractionSource: MutableInteractionSource,
    endInteractionSource: MutableInteractionSource,
    rawOffsetStart: State<Float>,
    rawOffsetEnd: State<Float>,
    enabled: Boolean,
    isRtl: Boolean,
    maxPx: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    gestureEndAction: State<(Boolean) -> Unit>,
    onDrag: (Boolean, Float) -> Unit,
): Modifier =
    if (enabled) {
        pointerInput(startInteractionSource, endInteractionSource, maxPx, isRtl, valueRange) {
            val rangeSliderLogic = RangeSliderLogic(
                startInteractionSource,
                endInteractionSource,
                rawOffsetStart,
                rawOffsetEnd,
                onDrag
            )
            coroutineScope {
                forEachGesture {
                    awaitPointerEventScope {
                        var thumbCaptured = false
                        // If we are dragging the start thumb, false if we are dragging end thumb.
                        var draggingStart = true
                        val pointerEvent = awaitFirstDown(requireUnconsumed = false)
                        val interaction = PressInteraction.Press(pointerEvent.position)
                        val slop = viewConfiguration.touchSlop
                        val posX =
                            if (isRtl) maxPx - pointerEvent.position.x else pointerEvent.position.x

                        if (abs(rawOffsetEnd.value - posX) > slop ||
                            abs(rawOffsetStart.value - posX) > slop
                        ) {
                            // We have enough distance we can start dragging right away
                            draggingStart = rangeSliderLogic.shouldCaptureStartThumb(posX)
                            rangeSliderLogic.captureThumb(
                                draggingStart,
                                posX,
                                interaction,
                                this@coroutineScope
                            )
                            thumbCaptured = true
                        }

                        awaitSlop(pointerEvent.id)?.let {
                            if (thumbCaptured) {
                                onDrag(draggingStart, if (isRtl) -it.second else it.second)
                            } else {
                                // Determine which thumb to drag based on the direction the user
                                // is dragging
                                val dir = it.second
                                draggingStart = if (isRtl) dir >= 0f else dir < 0f
                            }
                        }

                        if (!thumbCaptured) {
                            rangeSliderLogic.captureThumb(
                                draggingStart,
                                posX,
                                interaction,
                                this@coroutineScope
                            )
                        }

                        val finishInteraction = try {
                            val success = horizontalDrag(pointerId = pointerEvent.id) {
                                val deltaX = it.positionChange().x
                                onDrag(draggingStart, if (isRtl) -deltaX else deltaX)
                            }
                            if (success) {
                                PressInteraction.Release(interaction)
                            } else {
                                PressInteraction.Cancel(interaction)
                            }
                        } catch (e: CancellationException) {
                            PressInteraction.Cancel(interaction)
                        }

                        gestureEndAction.value.invoke(draggingStart)
                        launch {
                            rangeSliderLogic
                                .activeInteraction(draggingStart)
                                .emit(finishInteraction)
                        }
                    }
                }
            }
        }
    } else {
        this
    }

private class RangeSliderLogic(
    val startInteractionSource: MutableInteractionSource,
    val endInteractionSource: MutableInteractionSource,
    val rawOffsetStart: State<Float>,
    val rawOffsetEnd: State<Float>,
    val onDrag: (Boolean, Float) -> Unit,
) {
    fun activeInteraction(draggingStart: Boolean): MutableInteractionSource =
        if (draggingStart) startInteractionSource else endInteractionSource

    fun shouldCaptureStartThumb(eventX: Float): Boolean {
        val diffStart = abs(rawOffsetStart.value - eventX)
        val diffEnd = abs(rawOffsetEnd.value - eventX)
        return if (diffEnd == diffStart)
            rawOffsetStart.value > eventX
        else diffStart < diffEnd
    }

    fun captureThumb(
        draggingStart: Boolean,
        posX: Float,
        interaction: Interaction,
        scope: CoroutineScope
    ) {
        // TODO() interaction sources are not exposed in the public API so
        //  we only emit press events
        onDrag(
            draggingStart,
            posX - if (draggingStart) rawOffsetStart.value else rawOffsetEnd.value
        )
        scope.launch {
            activeInteraction(draggingStart).emit(interaction)
        }
    }
}

@Immutable
private class DefaultSliderColors(
    private val thumbColor: Color,
    private val disabledThumbColor: Color,
    private val activeTrackColor: Color,
    private val inactiveTrackColor: Color,
    private val disabledActiveTrackColor: Color,
    private val disabledInactiveTrackColor: Color,
    private val activeTickColor: Color,
    private val inactiveTickColor: Color,
    private val disabledActiveTickColor: Color,
    private val disabledInactiveTickColor: Color
) : SliderColors {

    @Composable
    override fun thumbColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) thumbColor else disabledThumbColor)
    }

    @Composable
    override fun trackColor(enabled: Boolean, active: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (active) activeTrackColor else inactiveTrackColor
            } else {
                if (active) disabledActiveTrackColor else disabledInactiveTrackColor
            }
        )
    }

    @Composable
    override fun tickColor(enabled: Boolean, active: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (active) activeTickColor else inactiveTickColor
            } else {
                if (active) disabledActiveTickColor else disabledInactiveTickColor
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultSliderColors

        if (thumbColor != other.thumbColor) return false
        if (disabledThumbColor != other.disabledThumbColor) return false
        if (activeTrackColor != other.activeTrackColor) return false
        if (inactiveTrackColor != other.inactiveTrackColor) return false
        if (disabledActiveTrackColor != other.disabledActiveTrackColor) return false
        if (disabledInactiveTrackColor != other.disabledInactiveTrackColor) return false
        if (activeTickColor != other.activeTickColor) return false
        if (inactiveTickColor != other.inactiveTickColor) return false
        if (disabledActiveTickColor != other.disabledActiveTickColor) return false
        if (disabledInactiveTickColor != other.disabledInactiveTickColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = thumbColor.hashCode()
        result = 31 * result + disabledThumbColor.hashCode()
        result = 31 * result + activeTrackColor.hashCode()
        result = 31 * result + inactiveTrackColor.hashCode()
        result = 31 * result + disabledActiveTrackColor.hashCode()
        result = 31 * result + disabledInactiveTrackColor.hashCode()
        result = 31 * result + activeTickColor.hashCode()
        result = 31 * result + inactiveTickColor.hashCode()
        result = 31 * result + disabledActiveTickColor.hashCode()
        result = 31 * result + disabledInactiveTickColor.hashCode()
        return result
    }
}

// Internal to be referred to in tests
//internal val ThumbRadius = 8.dp
private val ThumbRippleRadius = 24.dp
private val ThumbDefaultElevation = 1.dp
private val ThumbPressedElevation = 6.dp

// Internal to be referred to in tests
internal val TrackHeight = 4.dp
private val SliderHeight = 48.dp
private val SliderMinWidth = 144.dp
private val DefaultSliderConstraints =
    Modifier
        .widthIn(min = SliderMinWidth)
        .heightIn(max = SliderHeight)

private val SliderToTickAnimation = TweenSpec<Float>(durationMillis = 100)

private class SliderDraggableState(
    val onDelta: (Float) -> Unit
) : DraggableState {

    var isDragging by mutableStateOf(false)
        private set

    private val dragScope: DragScope = object : DragScope {
        override fun dragBy(pixels: Float): Unit = onDelta(pixels)
    }

    private val scrollMutex = MutatorMutex()

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit
    ): Unit = coroutineScope {
        isDragging = true
        scrollMutex.mutateWith(dragScope, dragPriority, block)
        isDragging = false
    }

    override fun dispatchRawDelta(delta: Float) {
        return onDelta(delta)
    }
}