package ua.hospes.lazygrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import androidx.compose.ui.util.fastForEach
import kotlin.math.min

internal interface LazyLayoutBeyondBoundsState {

    fun remeasure()

    val itemCount: Int

    val hasVisibleItems: Boolean

    val firstPlacedIndex: Int

    val lastPlacedIndex: Int
}

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyLayoutItemProvider.calculateLazyLayoutPinnedIndices(
    pinnedItemList: LazyLayoutPinnedItemList,
    beyondBoundsInfo: LazyLayoutBeyondBoundsInfo,
): List<Int> {
    if (!beyondBoundsInfo.hasIntervals() && pinnedItemList.isEmpty()) {
        return emptyList()
    } else {
        val pinnedItems = mutableListOf<Int>()
        val beyondBoundsRange = if (beyondBoundsInfo.hasIntervals()) {
            beyondBoundsInfo.start..min(beyondBoundsInfo.end, itemCount - 1)
        } else {
            IntRange.EMPTY
        }
        pinnedItemList.fastForEach {
            val index = findIndexByKey(it.key, it.index)
            if (index in beyondBoundsRange) return@fastForEach
            if (index !in 0 until itemCount) return@fastForEach
            pinnedItems.add(index)
        }
        for (i in beyondBoundsRange) {
            pinnedItems.add(i)
        }
        return pinnedItems
    }
}
