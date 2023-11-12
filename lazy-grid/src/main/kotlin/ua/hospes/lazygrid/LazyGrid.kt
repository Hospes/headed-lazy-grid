package ua.hospes.lazygrid

import androidx.compose.foundation.checkScrollableContainerConstraints
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun LazyGrid(
    /** Modifier to be applied for the inner layout */
    modifier: Modifier = Modifier,
    /** State controlling the scroll position */
    state: LazyGridState,
    /** Prefix sums of cross axis sizes of slots per line, e.g. the columns for vertical grid. */
    slots: LazyGridSlotsProvider,
    /** The inner padding to be added for the whole content (not for each individual item) */
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean = false,
    /** The layout orientation of the grid */
    isVertical: Boolean,
    /** fling behavior to be used for flinging */
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    /** Whether scrolling via the user gestures is allowed. */
    userScrollEnabled: Boolean,
    /** The vertical arrangement for items/lines. */
    verticalArrangement: Arrangement.Vertical,
    /** The horizontal arrangement for items/lines. */
    horizontalArrangement: Arrangement.Horizontal,
    /** The content of the grid */
    content: LazyGridScope.() -> Unit
) {
    val overscrollEffect = ScrollableDefaults.overscrollEffect()

    val itemProviderLambda = rememberLazyGridItemProviderLambda(state, content)

    val semanticState = rememberLazyGridSemanticState(state, reverseLayout)

    val coroutineScope = rememberCoroutineScope()
    val measurePolicy = rememberLazyGridMeasurePolicy(
        itemProviderLambda,
        state,
        slots,
        contentPadding,
        reverseLayout,
        isVertical,
        horizontalArrangement,
        verticalArrangement,
        coroutineScope
    )

    state.isVertical = isVertical

    ScrollPositionUpdater(itemProviderLambda, state)

    val orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal
    LazyLayout(
        modifier = modifier
            .then(state.remeasurementModifier)
            .then(state.awaitLayoutModifier)
            .lazyLayoutSemantics(
                itemProviderLambda = itemProviderLambda,
                state = semanticState,
                orientation = orientation,
                userScrollEnabled = userScrollEnabled,
                reverseScrolling = reverseLayout,
            )
            .clipScrollableContainer(orientation)
            .lazyLayoutBeyondBoundsModifier(
                state = rememberLazyGridBeyondBoundsState(state = state),
                beyondBoundsInfo = state.beyondBoundsInfo,
                reverseLayout = reverseLayout,
                layoutDirection = LocalLayoutDirection.current,
                orientation = orientation,
                enabled = userScrollEnabled
            )
            .overscroll(overscrollEffect)
            .scrollable(
                orientation = orientation,
                reverseDirection = ScrollableDefaults.reverseDirection(
                    LocalLayoutDirection.current,
                    orientation,
                    reverseLayout
                ),
                interactionSource = state.internalInteractionSource,
                flingBehavior = flingBehavior,
                state = state,
                overscrollEffect = overscrollEffect,
                enabled = userScrollEnabled
            ),
        prefetchState = state.prefetchState,
        measurePolicy = measurePolicy,
        itemProvider = itemProviderLambda
    )
}

/** Extracted to minimize the recomposition scope */
@Composable
private fun ScrollPositionUpdater(
    itemProviderLambda: () -> LazyGridItemProvider,
    state: LazyGridState
) {
    val itemProvider = itemProviderLambda()
    if (itemProvider.itemCount > 0) {
        state.updateScrollPositionIfTheFirstItemWasMoved(itemProvider)
    }
}

/** lazy grid slots configuration */
internal class LazyGridSlots(
    val sizes: IntArray,
    val positions: IntArray
)

@Composable
private fun rememberLazyGridMeasurePolicy(
    /** Items provider of the list. */
    itemProviderLambda: () -> LazyGridItemProvider,
    /** The state of the list. */
    state: LazyGridState,
    /** Prefix sums of cross axis sizes of slots of the grid. */
    slots: LazyGridSlotsProvider,
    /** The inner padding to be added for the whole content(nor for each individual item) */
    contentPadding: PaddingValues,
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean,
    /** The layout orientation of the list */
    isVertical: Boolean,
    /** The horizontal arrangement for items */
    horizontalArrangement: Arrangement.Horizontal?,
    /** The vertical arrangement for items */
    verticalArrangement: Arrangement.Vertical?,
    /** Coroutine scope for item animations */
    coroutineScope: CoroutineScope,
) = remember<LazyLayoutMeasureScope.(Constraints) -> MeasureResult>(
    state,
    slots,
    contentPadding,
    reverseLayout,
    isVertical,
    horizontalArrangement,
    verticalArrangement,
) {
    { containerConstraints ->
        checkScrollableContainerConstraints(
            containerConstraints,
            if (isVertical) Orientation.Vertical else Orientation.Horizontal
        )

        // resolve content paddings
        val startPadding =
            if (isVertical) {
                contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
            } else {
                // in horizontal configuration, padding is reversed by placeRelative
                contentPadding.calculateStartPadding(layoutDirection).roundToPx()
            }

        val endPadding =
            if (isVertical) {
                contentPadding.calculateRightPadding(layoutDirection).roundToPx()
            } else {
                // in horizontal configuration, padding is reversed by placeRelative
                contentPadding.calculateEndPadding(layoutDirection).roundToPx()
            }
        val topPadding = contentPadding.calculateTopPadding().roundToPx()
        val bottomPadding = contentPadding.calculateBottomPadding().roundToPx()
        val totalVerticalPadding = topPadding + bottomPadding
        val totalHorizontalPadding = startPadding + endPadding
        val totalMainAxisPadding = if (isVertical) totalVerticalPadding else totalHorizontalPadding
        val beforeContentPadding = when {
            isVertical && !reverseLayout -> topPadding
            isVertical && reverseLayout -> bottomPadding
            !isVertical && !reverseLayout -> startPadding
            else -> endPadding // !isVertical && reverseLayout
        }
        val afterContentPadding = totalMainAxisPadding - beforeContentPadding
        val contentConstraints =
            containerConstraints.offset(-totalHorizontalPadding, -totalVerticalPadding)

        val itemProvider = itemProviderLambda()
        val spanLayoutProvider = itemProvider.spanLayoutProvider
        val resolvedSlots = slots.invoke(density = this, constraints = containerConstraints)
        val slotsPerLine = resolvedSlots.sizes.size
        spanLayoutProvider.slotsPerLine = slotsPerLine

        // Update the state's cached Density and slotsPerLine
        state.density = this
        state.slotsPerLine = slotsPerLine

        val spaceBetweenLinesDp = if (isVertical) {
            requireNotNull(verticalArrangement) {
                "null verticalArrangement when isVertical == true"
            }.spacing
        } else {
            requireNotNull(horizontalArrangement) {
                "null horizontalArrangement when isVertical == false"
            }.spacing
        }
        val spaceBetweenLines = spaceBetweenLinesDp.roundToPx()
        val itemsCount = itemProvider.itemCount

        // can be negative if the content padding is larger than the max size from constraints
        val mainAxisAvailableSize = if (isVertical) {
            containerConstraints.maxHeight - totalVerticalPadding
        } else {
            containerConstraints.maxWidth - totalHorizontalPadding
        }
        val visualItemOffset = if (!reverseLayout || mainAxisAvailableSize > 0) {
            IntOffset(startPadding, topPadding)
        } else {
            // When layout is reversed and paddings together take >100% of the available space,
            // layout size is coerced to 0 when positioning. To take that space into account,
            // we offset start padding by negative space between paddings.
            IntOffset(
                if (isVertical) startPadding else startPadding + mainAxisAvailableSize,
                if (isVertical) topPadding + mainAxisAvailableSize else topPadding
            )
        }

        val measuredItemProvider = object : LazyGridMeasuredItemProvider(
            itemProvider,
            this,
            spaceBetweenLines
        ) {
            override fun createItem(
                index: Int,
                key: Any,
                contentType: Any?,
                crossAxisSize: Int,
                mainAxisSpacing: Int,
                placeables: List<Placeable>
            ) = LazyGridMeasuredItem(
                index = index,
                key = key,
                isVertical = isVertical,
                crossAxisSize = crossAxisSize,
                mainAxisSpacing = mainAxisSpacing,
                reverseLayout = reverseLayout,
                layoutDirection = layoutDirection,
                beforeContentPadding = beforeContentPadding,
                afterContentPadding = afterContentPadding,
                visualOffset = visualItemOffset,
                placeables = placeables,
                contentType = contentType,
                animator = state.placementAnimator
            )
        }
        val measuredLineProvider = object : LazyGridMeasuredLineProvider(
            isVertical = isVertical,
            slots = resolvedSlots,
            gridItemsCount = itemsCount,
            spaceBetweenLines = spaceBetweenLines,
            measuredItemProvider = measuredItemProvider,
            spanLayoutProvider = spanLayoutProvider,
        ) {
            override fun createLine(
                index: Int,
                items: Array<LazyGridMeasuredItem>,
                spans: List<GridItemSpan>,
                mainAxisSpacing: Int
            ) = LazyGridMeasuredLine(
                index = index,
                items = items,
                spans = spans,
                slots = resolvedSlots,
                isVertical = isVertical,
                mainAxisSpacing = mainAxisSpacing,
            )
        }
        state.prefetchInfoRetriever = { line ->
            val lineConfiguration = spanLayoutProvider.getLineConfiguration(line)
            var index = lineConfiguration.firstItemIndex
            var slot = 0
            val result = ArrayList<Pair<Int, Constraints>>(lineConfiguration.spans.size)
            lineConfiguration.spans.fastForEach {
                val span = it.currentLineSpan
                result.add(index to measuredLineProvider.childConstraints(slot, span))
                ++index
                slot += span
            }
            result
        }

        val firstVisibleLineIndex: Int
        val firstVisibleLineScrollOffset: Int

        Snapshot.withoutReadObservation {
            val index = state.updateScrollPositionIfTheFirstItemWasMoved(
                itemProvider, state.firstVisibleItemIndex
            )
            if (index < itemsCount || itemsCount <= 0) {
                firstVisibleLineIndex = spanLayoutProvider.getLineIndexOfItem(index)
                firstVisibleLineScrollOffset = state.firstVisibleItemScrollOffset
            } else {
                // the data set has been updated and now we have less items that we were
                // scrolled to before
                firstVisibleLineIndex = spanLayoutProvider.getLineIndexOfItem(itemsCount - 1)
                firstVisibleLineScrollOffset = 0
            }
        }

        val pinnedItems = itemProvider.calculateLazyLayoutPinnedIndices(
            state.pinnedItems,
            state.beyondBoundsInfo
        )

        measureLazyGrid(
            itemsCount = itemsCount,
            measuredLineProvider = measuredLineProvider,
            measuredItemProvider = measuredItemProvider,
            mainAxisAvailableSize = mainAxisAvailableSize,
            beforeContentPadding = beforeContentPadding,
            afterContentPadding = afterContentPadding,
            spaceBetweenLines = spaceBetweenLines,
            firstVisibleLineIndex = firstVisibleLineIndex,
            firstVisibleLineScrollOffset = firstVisibleLineScrollOffset,
            scrollToBeConsumed = state.scrollToBeConsumed,
            constraints = contentConstraints,
            isVertical = isVertical,
            headerIndexes = itemProvider.headerIndexes,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            reverseLayout = reverseLayout,
            density = this,
            placementAnimator = state.placementAnimator,
            spanLayoutProvider = spanLayoutProvider,
            pinnedItems = pinnedItems,
            coroutineScope = coroutineScope,
            layout = { width, height, placement ->
                layout(
                    containerConstraints.constrainWidth(width + totalHorizontalPadding),
                    containerConstraints.constrainHeight(height + totalVerticalPadding),
                    emptyMap(),
                    placement
                )
            }
        ).also {
            state.applyMeasureResult(it)
        }
    }
}