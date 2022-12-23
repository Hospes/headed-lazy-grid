package ua.hospes.lazygrid

import androidx.compose.foundation.lazy.layout.DelegatingLazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.rememberLazyNearestItemsRangeState
import androidx.compose.runtime.*

internal interface LazyGridItemProvider : LazyLayoutItemProvider {
    val spanLayoutProvider: LazyGridSpanLayoutProvider
    val hasCustomSpans: Boolean

    fun LazyGridItemSpanScope.getSpan(index: Int): GridItemSpan

    /** The list of indexes of the sticky header items */
    val headerIndexes: List<Int>
}

@Composable
internal fun rememberItemProvider(
    state: LazyGridState,
    content: LazyGridScope.() -> Unit,
): LazyGridItemProvider {
    val latestContent = rememberUpdatedState(content)
    val nearestItemsRangeState = rememberLazyNearestItemsRangeState(
        firstVisibleItemIndex = remember(state) {
            { state.firstVisibleItemIndex }
        },
        slidingWindowSize = { NearestItemsSlidingWindowSize },
        extraItemCount = { NearestItemsExtraItemCount }
    )

    return remember(nearestItemsRangeState) {
        val itemProviderState: State<LazyGridItemProvider> = derivedStateOf {
            val gridScope = LazyGridScopeImpl().apply(latestContent.value)
            LazyGridItemProviderImpl(
                gridScope.intervals,
                gridScope.headerIndexes,
                gridScope.hasCustomSpans,
                nearestItemsRangeState.value
            )
        }

        object : LazyGridItemProvider,
            LazyLayoutItemProvider by DelegatingLazyLayoutItemProvider(itemProviderState) {
            override val spanLayoutProvider: LazyGridSpanLayoutProvider
                get() = itemProviderState.value.spanLayoutProvider

            override val hasCustomSpans: Boolean
                get() = itemProviderState.value.hasCustomSpans

            override fun LazyGridItemSpanScope.getSpan(index: Int): GridItemSpan =
                with(itemProviderState.value) {
                    getSpan(index)
                }

            override val headerIndexes: List<Int>
                get() = itemProviderState.value.headerIndexes
        }
    }
}

private class LazyGridItemProviderImpl(
    private val intervals: IntervalList<LazyGridIntervalContent>,
    override val headerIndexes: List<Int>,
    override val hasCustomSpans: Boolean,
    nearestItemsRange: IntRange
) : LazyGridItemProvider, LazyLayoutItemProvider by LazyLayoutItemProvider(
    intervals = intervals,
    nearestItemsRange = nearestItemsRange,
    itemContent = { interval, index -> interval.item.invoke(LazyGridItemScopeImpl, index) }
) {
    override val spanLayoutProvider: LazyGridSpanLayoutProvider = LazyGridSpanLayoutProvider(this)

    override fun LazyGridItemSpanScope.getSpan(index: Int): GridItemSpan {
        val interval = intervals[index]
        val localIntervalIndex = index - interval.startIndex
        return interval.value.span.invoke(this, localIntervalIndex)
    }
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