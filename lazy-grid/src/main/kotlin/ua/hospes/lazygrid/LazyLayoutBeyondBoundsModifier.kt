package ua.hospes.lazygrid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun rememberLazyGridBeyondBoundsState(state: LazyGridState): LazyLayoutBeyondBoundsState {
    return remember(state) {
        LazyGridBeyondBoundsState(state)
    }
}

internal class LazyGridBeyondBoundsState(
    val state: LazyGridState,
) : LazyLayoutBeyondBoundsState {

    override fun remeasure() {
        state.remeasurement?.forceRemeasure()
    }

    override val itemCount: Int
        get() = state.layoutInfo.totalItemsCount
    override val hasVisibleItems: Boolean
        get() = state.layoutInfo.visibleItemsInfo.isNotEmpty()
    override val firstPlacedIndex: Int
        get() = state.firstVisibleItemIndex
    override val lastPlacedIndex: Int
        get() = state.layoutInfo.visibleItemsInfo.last().index
}