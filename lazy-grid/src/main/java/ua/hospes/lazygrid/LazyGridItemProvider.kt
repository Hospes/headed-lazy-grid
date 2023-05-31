package ua.hospes.lazygrid

import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnableItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

internal interface LazyGridItemProvider : LazyLayoutItemProvider {
    val keyIndexMap: LazyLayoutKeyIndexMap
    val spanLayoutProvider: LazyGridSpanLayoutProvider

    /** The list of indexes of the sticky header items */
    val headerIndexes: List<Int>
}

@Composable
internal fun rememberLazyGridItemProvider(
    state: LazyGridState,
    content: LazyGridScope.() -> Unit,
): LazyGridItemProvider {
    val latestContent = rememberUpdatedState(content)
    return remember(state) {
        LazyGridItemProviderImpl(
            state = state,
            latestContent = { latestContent.value },
        )
    }
}

private class LazyGridItemProviderImpl(
    private val state: LazyGridState,
    private val latestContent: () -> (LazyGridScope.() -> Unit)
) : LazyGridItemProvider {
    private val gridContent by derivedStateOf(referentialEqualityPolicy()) {
        LazyGridIntervalContent(latestContent())
    }

    override val keyIndexMap: LazyLayoutKeyIndexMap by NearestRangeKeyIndexMapState(
        firstVisibleItemIndex = { state.firstVisibleItemIndex },
        slidingWindowSize = { NearestItemsSlidingWindowSize },
        extraItemCount = { NearestItemsExtraItemCount },
        content = { gridContent }
    )

    override val itemCount: Int get() = gridContent.itemCount
    override val headerIndexes: List<Int> get() = gridContent.headerIndexes

    override fun getKey(index: Int): Any = keyIndexMap.getKey(index) ?: gridContent.getKey(index)

    override fun getContentType(index: Int): Any? = gridContent.getContentType(index)

    @Composable
    override fun Item(index: Int, key: Any) {
        LazyLayoutPinnableItem(key, index, state.pinnedItems) {
            gridContent.withInterval(index) { localIndex, content ->
                content.item(LazyGridItemScopeImpl, localIndex)
            }
        }
    }

    override val spanLayoutProvider: LazyGridSpanLayoutProvider
        get() = gridContent.spanLayoutProvider

    override fun getIndex(key: Any): Int = keyIndexMap.getIndex(key)
}

/**
 * We use the idea of sliding window as an optimization, so user can scroll up to this number of
 * items until we have to regenerate the key to index map.
 */
private const val NearestItemsSlidingWindowSize = 90

/**
 * The minimum amount of items near the current first visible item we want to have mapping for.
 */
private const val NearestItemsExtraItemCount = 200