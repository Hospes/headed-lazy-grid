package ua.hospes.lazygrid

import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Handles the item placement animations when it is set via
 * [LazyGridItemScope.animateItemPlacement].
 *
 * This class is responsible for detecting when item position changed, figuring our start/end
 * offsets and starting the animations.
 */
internal class LazyGridItemPlacementAnimator(
    private val scope: CoroutineScope,
    private val isVertical: Boolean
) {
    // state containing an animation and all relevant info for each item.
    private val keyToItemInfoMap = mutableMapOf<Any, ItemInfo>()

    // snapshot of the key to index map used for the last measuring.
    private var keyToIndexMap: Map<Any, Int> = emptyMap()

    // keeps the first and the last items positioned in the viewport and their visible part sizes.
    private var viewportStartItemIndex = -1
    private var viewportStartItemNotVisiblePartSize = 0
    private var viewportEndItemIndex = -1
    private var viewportEndItemNotVisiblePartSize = 0

    // stored to not allocate it every pass.
    private val positionedKeys = mutableSetOf<Any>()

    /**
     * Should be called after the measuring so we can detect position changes and start animations.
     *
     * Note that this method can compose new item and add it into the [positionedItems] list.
     */
    fun onMeasured(
        consumedScroll: Int,
        layoutWidth: Int,
        layoutHeight: Int,
        reverseLayout: Boolean,
        positionedItems: MutableList<LazyGridPositionedItem>,
        measuredItemProvider: LazyMeasuredItemProvider,
        spanLayoutProvider: LazyGridSpanLayoutProvider
    ) {
        if (!positionedItems.fastAny { it.hasAnimations } && keyToItemInfoMap.isEmpty()) {
            // no animations specified - no work needed
            reset()
            return
        }

        val mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth

        // the consumed scroll is considered as a delta we don't need to animate
        val notAnimatableDelta = (if (reverseLayout) -consumedScroll else consumedScroll).toOffset()

        val newFirstItem = positionedItems.first()
        val newLastItem = positionedItems.last()

        positionedItems.fastForEach { item ->
            val itemInfo = keyToItemInfoMap[item.key] ?: return@fastForEach
            itemInfo.index = item.index
            itemInfo.crossAxisSize = item.getCrossAxisSize()
            itemInfo.crossAxisOffset = item.getCrossAxisOffset()
        }

        val averageLineMainAxisSize = run {
            val lineOf: (Int) -> Int = {
                if (isVertical) positionedItems[it].row else positionedItems[it].column
            }

            var totalLinesMainAxisSize = 0
            var linesCount = 0

            var lineStartIndex = 0
            while (lineStartIndex < positionedItems.size) {
                val currentLine = lineOf(lineStartIndex)
                if (currentLine == -1) {
                    // Filter out exiting items.
                    ++lineStartIndex
                    continue
                }

                var lineMainAxisSize = 0
                var lineEndIndex = lineStartIndex
                while (lineEndIndex < positionedItems.size && lineOf(lineEndIndex) == currentLine) {
                    lineMainAxisSize = max(
                        lineMainAxisSize,
                        positionedItems[lineEndIndex].mainAxisSizeWithSpacings
                    )
                    ++lineEndIndex
                }

                totalLinesMainAxisSize += lineMainAxisSize
                ++linesCount

                lineStartIndex = lineEndIndex
            }

            totalLinesMainAxisSize / linesCount
        }

        positionedKeys.clear()
        // iterate through the items which are visible (without animated offsets)
        positionedItems.fastForEach { item ->
            positionedKeys.add(item.key)
            val itemInfo = keyToItemInfoMap[item.key]
            if (itemInfo == null) {
                // there is no state associated with this item yet
                if (item.hasAnimations) {
                    val newItemInfo = ItemInfo(
                        item.index,
                        item.getCrossAxisSize(),
                        item.getCrossAxisOffset()
                    )
                    val previousIndex = keyToIndexMap[item.key]
                    val offset = item.placeableOffset

                    val targetPlaceableOffsetMainAxis = if (previousIndex == null) {
                        // it is a completely new item. no animation is needed
                        offset.mainAxis
                    } else {
                        val fallback = if (!reverseLayout) {
                            offset.mainAxis
                        } else {
                            offset.mainAxis - item.mainAxisSizeWithSpacings
                        }
                        calculateExpectedOffset(
                            index = previousIndex,
                            mainAxisSizeWithSpacings = item.mainAxisSizeWithSpacings,
                            averageLineMainAxisSize = averageLineMainAxisSize,
                            scrolledBy = notAnimatableDelta,
                            fallback = fallback,
                            reverseLayout = reverseLayout,
                            mainAxisLayoutSize = mainAxisLayoutSize,
                            visibleItems = positionedItems,
                            spanLayoutProvider = spanLayoutProvider
                        )
                    }
                    val targetPlaceableOffset = if (isVertical) {
                        offset.copy(y = targetPlaceableOffsetMainAxis)
                    } else {
                        offset.copy(x = targetPlaceableOffsetMainAxis)
                    }

                    // populate placeable info list
                    repeat(item.placeablesCount) { placeableIndex ->
                        newItemInfo.placeables.add(
                            PlaceableInfo(
                                targetPlaceableOffset,
                                item.getMainAxisSize(placeableIndex)
                            )
                        )
                    }
                    keyToItemInfoMap[item.key] = newItemInfo
                    startAnimationsIfNeeded(item, newItemInfo)
                }
            } else {
                if (item.hasAnimations) {
                    // apply new not animatable offset
                    itemInfo.notAnimatableDelta += notAnimatableDelta
                    startAnimationsIfNeeded(item, itemInfo)
                } else {
                    // no animation, clean up if needed
                    keyToItemInfoMap.remove(item.key)
                }
            }
        }

        // previously we were animating items which are visible in the end state so we had to
        // compare the current state with the state used for the previous measuring.
        // now we will animate disappearing items so the current state is their starting state
        // so we can update current viewport start/end items

        if (!reverseLayout) {
            viewportStartItemIndex = newFirstItem.index
            viewportStartItemNotVisiblePartSize = newFirstItem.offset.mainAxis
            viewportEndItemIndex = newLastItem.index
            viewportEndItemNotVisiblePartSize = newLastItem.offset.mainAxis +
                    newLastItem.lineMainAxisSizeWithSpacings - mainAxisLayoutSize
        } else {
            viewportStartItemIndex = newLastItem.index
            viewportStartItemNotVisiblePartSize = mainAxisLayoutSize -
                    newLastItem.offset.mainAxis - newLastItem.lineMainAxisSize
            viewportEndItemIndex = newFirstItem.index
            viewportEndItemNotVisiblePartSize = -newFirstItem.offset.mainAxis +
                    (newFirstItem.lineMainAxisSizeWithSpacings -
                            if (isVertical) newFirstItem.size.height else newFirstItem.size.width)
        }

        val iterator = keyToItemInfoMap.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!positionedKeys.contains(entry.key)) {
                // found an item which was in our map previously but is not a part of the
                // positionedItems now
                val itemInfo = entry.value
                // apply new not animatable delta for this item
                itemInfo.notAnimatableDelta += notAnimatableDelta

                val index = measuredItemProvider.keyToIndexMap[entry.key]

                // whether at least one placeable is within the viewport bounds.
                // this usually means that we will start animation for it right now
                val withinBounds = itemInfo.placeables.fastAny {
                    val currentTarget = it.targetOffset + itemInfo.notAnimatableDelta
                    currentTarget.mainAxis + it.mainAxisSize > 0 &&
                            currentTarget.mainAxis < mainAxisLayoutSize
                }

                // whether the animation associated with the item has been finished
                val isFinished = !itemInfo.placeables.fastAny { it.inProgress }

                if ((!withinBounds && isFinished) ||
                    index == null ||
                    itemInfo.placeables.isEmpty()
                ) {
                    iterator.remove()
                } else {
                    // not sure if this item will end up on the last line or not. assume not,
                    // therefore leave the mainAxisSpacing to be the default one
                    val measuredItem = measuredItemProvider.getAndMeasure(
                        index = ItemIndex(index),
                        constraints = if (isVertical) {
                            Constraints.fixedWidth(itemInfo.crossAxisSize)
                        } else {
                            Constraints.fixedHeight(itemInfo.crossAxisSize)
                        }
                    )

                    // calculate the target offset for the animation.
                    val absoluteTargetOffset = calculateExpectedOffset(
                        index = index,
                        mainAxisSizeWithSpacings = measuredItem.mainAxisSizeWithSpacings,
                        averageLineMainAxisSize = averageLineMainAxisSize,
                        scrolledBy = notAnimatableDelta,
                        fallback = mainAxisLayoutSize,
                        reverseLayout = reverseLayout,
                        mainAxisLayoutSize = mainAxisLayoutSize,
                        visibleItems = positionedItems,
                        spanLayoutProvider = spanLayoutProvider
                    )
                    val targetOffset = if (reverseLayout) {
                        mainAxisLayoutSize - absoluteTargetOffset - measuredItem.mainAxisSize
                    } else {
                        absoluteTargetOffset
                    }

                    val item = measuredItem.position(
                        targetOffset,
                        itemInfo.crossAxisOffset,
                        layoutWidth,
                        layoutHeight,
                        LazyGridItemInfo.UnknownRow,
                        LazyGridItemInfo.UnknownColumn,
                        measuredItem.mainAxisSize
                    )
                    positionedItems.add(item)
                    startAnimationsIfNeeded(item, itemInfo)
                }
            }
        }

        keyToIndexMap = measuredItemProvider.keyToIndexMap
    }

    /**
     * Returns the current animated item placement offset. By calling it only during the layout
     * phase we can skip doing remeasure on every animation frame.
     */
    fun getAnimatedOffset(
        key: Any,
        placeableIndex: Int,
        minOffset: Int,
        maxOffset: Int,
        rawOffset: IntOffset
    ): IntOffset {
        val itemInfo = keyToItemInfoMap[key] ?: return rawOffset
        val item = itemInfo.placeables[placeableIndex]
        val currentValue = item.animatedOffset.value + itemInfo.notAnimatableDelta
        val currentTarget = item.targetOffset + itemInfo.notAnimatableDelta

        // cancel the animation if it is fully out of the bounds.
        if (item.inProgress &&
            ((currentTarget.mainAxis < minOffset && currentValue.mainAxis < minOffset) ||
                    (currentTarget.mainAxis > maxOffset && currentValue.mainAxis > maxOffset))
        ) {
            scope.launch {
                item.animatedOffset.snapTo(item.targetOffset)
                item.inProgress = false
            }
        }

        return currentValue
    }

    /**
     * Should be called when the animations are not needed for the next positions change,
     * for example when we snap to a new position.
     */
    fun reset() {
        keyToItemInfoMap.clear()
        keyToIndexMap = emptyMap()
        viewportStartItemIndex = -1
        viewportStartItemNotVisiblePartSize = 0
        viewportEndItemIndex = -1
        viewportEndItemNotVisiblePartSize = 0
    }

    /**
     * Estimates the outside of the viewport offset for the item. Used to understand from
     * where to start animation for the item which wasn't visible previously or where it should
     * end for the item which is not going to be visible in the end.
     */
    private fun calculateExpectedOffset(
        index: Int,
        mainAxisSizeWithSpacings: Int,
        averageLineMainAxisSize: Int,
        scrolledBy: IntOffset,
        reverseLayout: Boolean,
        mainAxisLayoutSize: Int,
        fallback: Int,
        visibleItems: List<LazyGridPositionedItem>,
        spanLayoutProvider: LazyGridSpanLayoutProvider
    ): Int {
        val afterViewportEnd =
            if (!reverseLayout) viewportEndItemIndex < index else viewportEndItemIndex > index
        val beforeViewportStart =
            if (!reverseLayout) viewportStartItemIndex > index else viewportStartItemIndex < index
        return when {
            afterViewportEnd -> {
                val fromIndex = spanLayoutProvider.firstIndexInNextLineAfter(
                    if (!reverseLayout) viewportEndItemIndex else index
                )
                val toIndex = spanLayoutProvider.lastIndexInPreviousLineBefore(
                    if (!reverseLayout) index else viewportEndItemIndex
                )
                mainAxisLayoutSize + viewportEndItemNotVisiblePartSize + scrolledBy.mainAxis +
                        // add sizes of the lines between the last visible one and this one.
                        spanLayoutProvider.getLinesMainAxisSizesSum(
                            fromIndex = fromIndex,
                            toIndex = toIndex,
                            averageLineMainAxisSize = averageLineMainAxisSize,
                            visibleItems = visibleItems
                        )
            }
            beforeViewportStart -> {
                val fromIndex = spanLayoutProvider.firstIndexInNextLineAfter(
                    if (!reverseLayout) index else viewportStartItemIndex
                )
                val toIndex = spanLayoutProvider.lastIndexInPreviousLineBefore(
                    if (!reverseLayout) viewportStartItemIndex else index
                )
                viewportStartItemNotVisiblePartSize + scrolledBy.mainAxis +
                        // minus the size of this item as we are looking for the start offset of it.
                        -mainAxisSizeWithSpacings +
                        // minus sizes of the lines between the first visible one and this one.
                        -spanLayoutProvider.getLinesMainAxisSizesSum(
                            fromIndex = fromIndex,
                            toIndex = toIndex,
                            averageLineMainAxisSize = averageLineMainAxisSize,
                            visibleItems = visibleItems
                        )
            }
            else -> {
                fallback
            }
        }
    }

    private fun startAnimationsIfNeeded(item: LazyGridPositionedItem, itemInfo: ItemInfo) {
        // first we make sure our item info is up to date (has the item placeables count)
        while (itemInfo.placeables.size > item.placeablesCount) {
            itemInfo.placeables.removeLast()
        }
        while (itemInfo.placeables.size < item.placeablesCount) {
            val newPlaceableInfoIndex = itemInfo.placeables.size
            val rawOffset = item.offset
            itemInfo.placeables.add(
                PlaceableInfo(
                    rawOffset - itemInfo.notAnimatableDelta,
                    item.getMainAxisSize(newPlaceableInfoIndex)
                )
            )
        }

        itemInfo.placeables.fastForEachIndexed { index, placeableInfo ->
            val currentTarget = placeableInfo.targetOffset + itemInfo.notAnimatableDelta
            val currentOffset = item.placeableOffset
            placeableInfo.mainAxisSize = item.getMainAxisSize(index)
            val animationSpec = item.getAnimationSpec(index)
            if (currentTarget != currentOffset) {
                placeableInfo.targetOffset = currentOffset - itemInfo.notAnimatableDelta
                if (animationSpec != null) {
                    placeableInfo.inProgress = true
                    scope.launch {
                        val finalSpec = if (placeableInfo.animatedOffset.isRunning) {
                            // when interrupted, use the default spring, unless the spec is a spring.
                            if (animationSpec is SpringSpec<IntOffset>) animationSpec else
                                InterruptionSpec
                        } else {
                            animationSpec
                        }

                        try {
                            placeableInfo.animatedOffset.animateTo(
                                placeableInfo.targetOffset,
                                finalSpec
                            )
                            placeableInfo.inProgress = false
                        } catch (_: CancellationException) {
                            // we don't reset inProgress in case of cancellation as it means
                            // there is a new animation started which would reset it later
                        }
                    }
                }
            }
        }
    }

    private fun Int.toOffset() =
        IntOffset(if (isVertical) 0 else this, if (!isVertical) 0 else this)

    private val IntOffset.mainAxis get() = if (isVertical) y else x
}

private class ItemInfo(
    var index: Int,
    var crossAxisSize: Int,
    var crossAxisOffset: Int
) {
    var notAnimatableDelta: IntOffset = IntOffset.Zero
    val placeables = mutableListOf<PlaceableInfo>()
}

private class PlaceableInfo(initialOffset: IntOffset, var mainAxisSize: Int) {
    val animatedOffset = Animatable(initialOffset, IntOffset.VectorConverter)
    var targetOffset: IntOffset = initialOffset
    var inProgress by mutableStateOf(false)
}

/**
 * We switch to this spec when a duration based animation is being interrupted.
 */
private val InterruptionSpec = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.VisibilityThreshold
)

private fun LazyGridSpanLayoutProvider.getLinesMainAxisSizesSum(
    fromIndex: Int,
    toIndex: Int,
    averageLineMainAxisSize: Int,
    visibleItems: List<LazyGridPositionedItem>
): Int {
    var index = fromIndex
    var sizes = 0
    while (index <= toIndex) {
        val lastItemInTheLine = firstIndexInNextLineAfter(index) - 1
        if (lastItemInTheLine <= toIndex) {
            sizes += visibleItems.getLineSize(lastItemInTheLine, averageLineMainAxisSize)
        }
        index = lastItemInTheLine + 1
    }
    return sizes
}

private fun List<LazyGridPositionedItem>.getLineSize(itemIndex: Int, fallback: Int): Int {
    if (isEmpty() || itemIndex < first().index || itemIndex > last().index) return fallback
    if ((itemIndex - first().index) < (last().index - itemIndex)) {
        for (index in indices) {
            val item = get(index)
            if (item.index == itemIndex) return item.lineMainAxisSizeWithSpacings
            if (item.index > itemIndex) break
        }
    } else {
        for (index in lastIndex downTo 0) {
            val item = get(index)
            if (item.index == itemIndex) return item.lineMainAxisSizeWithSpacings
            if (item.index < itemIndex) break
        }
    }
    return fallback
}

private fun LazyGridSpanLayoutProvider.lastIndexInPreviousLineBefore(index: Int) =
    firstIndexInLineContaining(index) - 1

private fun LazyGridSpanLayoutProvider.firstIndexInNextLineAfter(index: Int) =
    if (index >= totalSize) {
        // after totalSize we just approximate with 1 slot per item
        firstIndexInLineContaining(index) + slotsPerLine
    } else {
        val lineIndex = getLineIndexOfItem(index)
        val lineConfiguration = getLineConfiguration(lineIndex.value)
        lineConfiguration.firstItemIndex + lineConfiguration.spans.size
    }

private fun LazyGridSpanLayoutProvider.firstIndexInLineContaining(index: Int): Int {
    return if (index >= totalSize) {
        val firstIndexForLastKnowLine = getFirstIndexInNextLineAfterTheLastKnownOne()
        // after totalSize we just approximate with 1 slot per item
        val linesBetween = (index - firstIndexForLastKnowLine) / slotsPerLine
        firstIndexForLastKnowLine + slotsPerLine * linesBetween
    } else {
        val lineIndex = getLineIndexOfItem(index)
        val lineConfiguration = getLineConfiguration(lineIndex.value)
        lineConfiguration.firstItemIndex
    }
}

private fun LazyGridSpanLayoutProvider.getFirstIndexInNextLineAfterTheLastKnownOne(): Int {
    // first we find the line for the `totalSize - 1` item
    val lineConfiguration = getLineConfiguration(getLineIndexOfItem(totalSize - 1).value)
    var currentSpan = 0
    var currentIndex = lineConfiguration.firstItemIndex - 1
    // then we go through all the known spans
    lineConfiguration.spans.fastForEach {
        currentSpan += it.currentLineSpan
        currentIndex++
    }
    // and increment index as if we had more items with slot == 1 until we switch to the next line
    currentIndex += slotsPerLine - currentSpan + 1
    return currentIndex
}