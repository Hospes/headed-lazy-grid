package ua.hospes.lazygrid

import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot

/**
 * Contains the current scroll position represented by the first visible item index and the first
 * visible item scroll offset.
 */
internal class LazyGridScrollPosition(
    initialIndex: Int = 0,
    initialScrollOffset: Int = 0
) {
    var index by mutableIntStateOf(initialIndex)
        private set

    var scrollOffset by mutableIntStateOf(initialScrollOffset)
        private set

    private var hadFirstNotEmptyLayout = false

    /** The last known key of the first item at [index] line. */
    private var lastKnownFirstItemKey: Any? = null

    /**
     * Updates the current scroll position based on the results of the last measurement.
     */
    fun updateFromMeasureResult(measureResult: LazyGridMeasureResult) {
        lastKnownFirstItemKey = measureResult.firstVisibleLine?.items?.firstOrNull()?.key
        // we ignore the index and offset from measureResult until we get at least one
        // measurement with real items. otherwise the initial index and scroll passed to the
        // state would be lost and overridden with zeros.
        if (hadFirstNotEmptyLayout || measureResult.totalItemsCount > 0) {
            hadFirstNotEmptyLayout = true
            val scrollOffset = measureResult.firstVisibleLineScrollOffset
            check(scrollOffset >= 0f) { "scrollOffset should be non-negative ($scrollOffset)" }

            Snapshot.withoutReadObservation {
                update(
                    measureResult.firstVisibleLine?.items?.firstOrNull()?.index ?: 0,
                    scrollOffset
                )
            }
        }
    }

    /**
     * Updates the scroll position - the passed values will be used as a start position for
     * composing the items during the next measure pass and will be updated by the real
     * position calculated during the measurement. This means that there is guarantee that
     * exactly this index and offset will be applied as it is possible that:
     * a) there will be no item at this index in reality
     * b) item at this index will be smaller than the asked scrollOffset, which means we would
     * switch to the next item
     * c) there will be not enough items to fill the viewport after the requested index, so we
     * would have to compose few elements before the asked index, changing the first visible item.
     */
    fun requestPosition(index: Int, scrollOffset: Int) {
        update(index, scrollOffset)
        // clear the stored key as we have a direct request to scroll to [index] position and the
        // next [checkIfFirstVisibleItemWasMoved] shouldn't override this.
        lastKnownFirstItemKey = null
    }

    /**
     * In addition to keeping the first visible item index we also store the key of this item.
     * When the user provided custom keys for the items this mechanism allows us to detect when
     * there were items added or removed before our current first visible item and keep this item
     * as the first visible one even given that its index has been changed.
     */
    fun updateScrollPositionIfTheFirstItemWasMoved(itemProvider: LazyGridItemProvider) {
        Snapshot.withoutReadObservation {
            update(
                itemProvider.findIndexByKey(lastKnownFirstItemKey, index),
                scrollOffset
            )
        }
    }

    private fun update(index: Int, scrollOffset: Int) {
        require(index >= 0f) { "Index should be non-negative ($index)" }
        if (index != this.index) {
            this.index = index
        }
        if (scrollOffset != this.scrollOffset) {
            this.scrollOffset = scrollOffset
        }
    }
}

/**
 * Finds a position of the item with the given key in the lists. This logic allows us to
 * detect when there were items added or removed before our current first item.
 */
fun LazyLayoutItemProvider.findIndexByKey(
    key: Any?,
    lastKnownIndex: Int,
): Int {
    if (key == null) {
        // there were no real item during the previous measure
        return lastKnownIndex
    }
    if (lastKnownIndex < itemCount &&
        key == getKey(lastKnownIndex)
    ) {
        // this item is still at the same index
        return lastKnownIndex
    }
    val newIndex = getIndex(key)
    if (newIndex != -1) {
        return newIndex
    }
    // fallback to the previous index if we don't know the new index of the item
    return lastKnownIndex
}