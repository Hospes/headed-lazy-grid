package ua.hospes.lazygrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.ScrollAxisRange

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberLazyGridSemanticState(
    state: LazyGridState,
    itemProvider: LazyLayoutItemProvider,
    reverseScrolling: Boolean
): LazyLayoutSemanticState =
    remember(state, itemProvider, reverseScrolling) {
        object : LazyLayoutSemanticState {
            override fun scrollAxisRange(): ScrollAxisRange =
                ScrollAxisRange(
                    value = {
                        // This is a simple way of representing the current position without
                        // needing any lazy items to be measured. It's good enough so far, because
                        // screen-readers care mostly about whether scroll position changed or not
                        // rather than the actual offset in pixels.
                        state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset / 100_000f
                    },
                    maxValue = {
                        if (state.canScrollForward) {
                            // If we can scroll further, we don't know the end yet,
                            // but it's upper bounded by #items + 1
                            itemProvider.itemCount + 1f
                        } else {
                            // If we can't scroll further, the current value is the max
                            state.firstVisibleItemIndex +
                                    state.firstVisibleItemScrollOffset / 100_000f
                        }
                    },
                    reverseScrolling = reverseScrolling
                )

            override suspend fun animateScrollBy(delta: Float) {
                state.animateScrollBy(delta)
            }

            override suspend fun scrollToItem(index: Int) {
                state.scrollToItem(index)
            }

            // TODO(popam): check if this is correct - it would be nice to provide correct columns
            override fun collectionInfo(): CollectionInfo =
                CollectionInfo(rowCount = -1, columnCount = -1)
        }
    }