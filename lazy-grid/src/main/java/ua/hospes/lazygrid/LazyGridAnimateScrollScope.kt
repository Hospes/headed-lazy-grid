package ua.hospes.lazygrid

import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.abs
import kotlin.math.max

internal class LazyGridAnimateScrollScope(
    private val state: LazyGridState
) : LazyAnimateScrollScope {
    override val density: Density get() = state.density

    override val firstVisibleItemIndex: Int get() = state.firstVisibleItemIndex

    override val firstVisibleItemScrollOffset: Int get() = state.firstVisibleItemScrollOffset

    override val lastVisibleItemIndex: Int
        get() = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

    override val itemCount: Int get() = state.layoutInfo.totalItemsCount

    override fun getTargetItemOffset(index: Int): Int? =
        state.layoutInfo.visibleItemsInfo
            .fastFirstOrNull {
                it.index == index
            }?.let { item ->
                if (state.isVertical) {
                    item.offset.y
                } else {
                    item.offset.x
                }
            }

    override fun ScrollScope.snapToItem(index: Int, scrollOffset: Int) {
        state.snapToItemIndexInternal(index, scrollOffset)
    }

    override fun expectedDistanceTo(index: Int, targetScrollOffset: Int): Float {
        val visibleItems = state.layoutInfo.visibleItemsInfo
        val slotsPerLine = state.slotsPerLine
        val averageLineMainAxisSize = calculateLineAverageMainAxisSize(
            visibleItems,
            state.isVertical
        )
        val before = index < firstVisibleItemIndex
        val linesDiff =
            (index - firstVisibleItemIndex + (slotsPerLine - 1) * if (before) -1 else 1) /
                    slotsPerLine

        var coercedOffset = minOf(abs(targetScrollOffset), averageLineMainAxisSize)
        if (targetScrollOffset < 0) coercedOffset *= -1
        return (averageLineMainAxisSize * linesDiff).toFloat() +
                coercedOffset - firstVisibleItemScrollOffset
    }

    override val numOfItemsForTeleport: Int get() = 100 * state.slotsPerLine

    private fun calculateLineAverageMainAxisSize(
        visibleItems: List<LazyGridItemInfo>,
        isVertical: Boolean
    ): Int {
        val lineOf: (Int) -> Int = {
            if (isVertical) visibleItems[it].row else visibleItems[it].column
        }

        var totalLinesMainAxisSize = 0
        var linesCount = 0

        var lineStartIndex = 0
        while (lineStartIndex < visibleItems.size) {
            val currentLine = lineOf(lineStartIndex)
            if (currentLine == -1) {
                // Filter out exiting items.
                ++lineStartIndex
                continue
            }

            var lineMainAxisSize = 0
            var lineEndIndex = lineStartIndex
            while (lineEndIndex < visibleItems.size && lineOf(lineEndIndex) == currentLine) {
                lineMainAxisSize = max(
                    lineMainAxisSize,
                    if (isVertical) {
                        visibleItems[lineEndIndex].size.height
                    } else {
                        visibleItems[lineEndIndex].size.width
                    }
                )
                ++lineEndIndex
            }

            totalLinesMainAxisSize += lineMainAxisSize
            ++linesCount

            lineStartIndex = lineEndIndex
        }

        return totalLinesMainAxisSize / linesCount
    }

    override suspend fun scroll(block: suspend ScrollScope.() -> Unit) {
        state.scroll(block = block)
    }
}