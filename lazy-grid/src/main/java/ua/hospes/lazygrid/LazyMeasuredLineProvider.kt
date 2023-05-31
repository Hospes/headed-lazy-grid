package ua.hospes.lazygrid

import androidx.compose.ui.unit.Constraints

/**
 * Abstracts away subcomposition and span calculation from the measuring logic of entire lines.
 */
internal class LazyMeasuredLineProvider(
    private val isVertical: Boolean,
    private val slotSizesSums: List<Int>,
    private val crossAxisSpacing: Int,
    private val gridItemsCount: Int,
    private val spaceBetweenLines: Int,
    private val measuredItemProvider: LazyMeasuredItemProvider,
    private val spanLayoutProvider: LazyGridSpanLayoutProvider,
    private val measuredLineFactory: MeasuredLineFactory
) {
    // The constraints for cross axis size. The main axis is not restricted.
    internal fun childConstraints(startSlot: Int, span: Int): Constraints {
        val lastSlotSum = slotSizesSums[startSlot + span - 1]
        val prevSlotSum = if (startSlot == 0) 0 else slotSizesSums[startSlot - 1]
        val slotsSize = lastSlotSum - prevSlotSum
        val crossAxisSize = (slotsSize + crossAxisSpacing * (span - 1)).coerceAtLeast(0)
        return if (isVertical) {
            Constraints.fixedWidth(crossAxisSize)
        } else {
            Constraints.fixedHeight(crossAxisSize)
        }
    }

    fun itemConstraints(itemIndex: ItemIndex): Constraints {
        val span = spanLayoutProvider.spanOf(
            itemIndex.value,
            spanLayoutProvider.slotsPerLine
        )
        return childConstraints(0, span)
    }

    /**
     * Used to subcompose items on lines of lazy grids. Composed placeables will be measured
     * with the correct constraints and wrapped into [LazyGridMeasuredLine].
     */
    fun getAndMeasure(lineIndex: LineIndex): LazyGridMeasuredLine {
        val lineConfiguration = spanLayoutProvider.getLineConfiguration(lineIndex.value)
        val lineItemsCount = lineConfiguration.spans.size

        // we add space between lines as an extra spacing for all lines apart from the last one
        // so the lazy grid measuring logic will take it into account.
        val mainAxisSpacing = if (lineItemsCount == 0 ||
            lineConfiguration.firstItemIndex + lineItemsCount == gridItemsCount) {
            0
        } else {
            spaceBetweenLines
        }

        var startSlot = 0
        val items = Array(lineItemsCount) {
            val span = lineConfiguration.spans[it].currentLineSpan
            val constraints = childConstraints(startSlot, span)
            measuredItemProvider.getAndMeasure(
                ItemIndex(lineConfiguration.firstItemIndex + it),
                mainAxisSpacing,
                constraints
            ).also { startSlot += span }
        }
        return measuredLineFactory.createLine(
            lineIndex,
            items,
            lineConfiguration.spans,
            mainAxisSpacing
        )
    }

    /**
     * Contains the mapping between the key and the index. It could contain not all the items of
     * the list as an optimization.
     **/
    val keyToIndexMap: Map<Any, Int> get() = measuredItemProvider.keyToIndexMap
}

// This interface allows to avoid autoboxing on index param
internal fun interface MeasuredLineFactory {
    fun createLine(
        index: LineIndex,
        items: Array<LazyGridMeasuredItem>,
        spans: List<GridItemSpan>,
        mainAxisSpacing: Int
    ): LazyGridMeasuredLine
}