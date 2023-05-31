package ua.hospes.lazygrid

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEachIndexed

/**
 * This method finds the sticky header in composedItems list or composes the header item if needed.
 *
 * @param composedVisibleItems list of items already composed and expected to be visible. if the
 * header wasn't in this list but is needed the header will be added as the first item in this list.
 * @param itemProvider the provider so we can compose a header if it wasn't composed already
 * @param headerIndexes list of indexes of headers. Must be sorted.
 * @param beforeContentPadding the padding before the first item in the list
 */
internal fun findOrComposeLazyGridHeader(
    composedVisibleItems: MutableList<LazyGridPositionedItem>,
    measuredItemProvider: LazyGridMeasuredItemProvider,
    headerIndexes: List<Int>,
    isVertical: Boolean,
    beforeContentPadding: Int,
    layoutWidth: Int,
    layoutHeight: Int,
): LazyGridPositionedItem? {
    var currentHeaderOffset: Int = Int.MIN_VALUE
    var currentHeaderCrossAxisOffset: Int = Int.MIN_VALUE
    var currentHeaderCrossAxisSize: Int = 0
    var nextHeaderOffset: Int = Int.MIN_VALUE

    var currentHeaderListPosition = -1
    var nextHeaderListPosition = -1
    // we use visibleItemsInfo and not firstVisibleItemIndex as visibleItemsInfo list also
    // contains all the items which are visible in the start content padding area
    val firstVisible = composedVisibleItems.first().index
    // find the header which can be displayed
    for (index in headerIndexes.indices) {
        if (headerIndexes[index] <= firstVisible) {
            currentHeaderListPosition = headerIndexes[index]
            nextHeaderListPosition = headerIndexes.getOrElse(index + 1) { -1 }
        } else {
            break
        }
    }

    var indexInComposedVisibleItems = -1
    composedVisibleItems.fastForEachIndexed { index, item ->
        if (item.index == currentHeaderListPosition) {
            indexInComposedVisibleItems = index
            currentHeaderOffset = item.getMainAxisOffset()
            currentHeaderCrossAxisOffset = item.getCrossAxisOffset()
            currentHeaderCrossAxisSize = item.getCrossAxisSize()
        } else {
            if (item.index == nextHeaderListPosition) {
                nextHeaderOffset = item.getMainAxisOffset()
            }
        }
    }

    if (currentHeaderListPosition == -1) {
        // we have no headers needing special handling
        return null
    }

    val measuredHeaderItem = measuredItemProvider.getAndMeasure(
        index = currentHeaderListPosition,
        constraints = if (isVertical) {
            Constraints.fixedWidth(layoutWidth)
        } else {
            Constraints.fixedHeight(layoutHeight)
        }
    )

    var headerOffset = if (currentHeaderOffset != Int.MIN_VALUE) {
        maxOf(-beforeContentPadding, currentHeaderOffset)
    } else {
        -beforeContentPadding
    }
    // if we have a next header overlapping with the current header, the next one will be
    // pushing the current one away from the viewport.
    if (nextHeaderOffset != Int.MIN_VALUE) {
        headerOffset = minOf(headerOffset, nextHeaderOffset - measuredHeaderItem.mainAxisSize)
    }
    var headerCrossAxisOffset = currentHeaderCrossAxisOffset

    return measuredHeaderItem.position(
        mainAxisOffset = headerOffset,
        crossAxisOffset = 0,//headerCrossAxisOffset,
        layoutWidth = layoutWidth,
        layoutHeight = layoutHeight,
        row = LazyGridItemInfo.UnknownRow,
        column = LazyGridItemInfo.UnknownColumn,
        //lineMainAxisSize = measuredHeaderItem.mainAxisSize
    ).also {
        if (indexInComposedVisibleItems != -1) {
            composedVisibleItems[indexInComposedVisibleItems] = it
        } else {
            composedVisibleItems.add(0, it)
        }
    }
}