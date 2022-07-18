package ua.hospes.lazygrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.IntSize

/**
 * The result of the measure pass for lazy list layout.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class LazyGridMeasureResult(
    // properties defining the scroll position:
    /** The new first visible line of items.*/
    val firstVisibleLine: LazyMeasuredLine?,
    /** The new value for [LazyGridState.firstVisibleItemScrollOffset].*/
    val firstVisibleLineScrollOffset: Int,
    /** True if there is some space available to continue scrolling in the forward direction.*/
    val canScrollForward: Boolean,
    /** The amount of scroll consumed during the measure pass.*/
    val consumedScroll: Float,
    /** MeasureResult defining the layout.*/
    measureResult: MeasureResult,
    // properties representing the info needed for LazyListLayoutInfo:
    /** see [LazyGridLayoutInfo.visibleItemsInfo] */
    override val visibleItemsInfo: List<LazyGridItemInfo>,
    /** see [LazyGridLayoutInfo.viewportStartOffset] */
    override val viewportStartOffset: Int,
    /** see [LazyGridLayoutInfo.viewportEndOffset] */
    override val viewportEndOffset: Int,
    /** see [LazyGridLayoutInfo.totalItemsCount] */
    override val totalItemsCount: Int,
    /** see [LazyGridLayoutInfo.reverseLayout] */
    override val reverseLayout: Boolean,
    /** see [LazyGridLayoutInfo.orientation] */
    override val orientation: Orientation,
    /** see [LazyGridLayoutInfo.afterContentPadding] */
    override val afterContentPadding: Int
) : LazyGridLayoutInfo, MeasureResult by measureResult {
    override val viewportSize: IntSize
        get() = IntSize(width, height)
    override val beforeContentPadding: Int get() = -viewportStartOffset
}