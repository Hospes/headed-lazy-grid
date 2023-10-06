package ua.hospes.lazygrid

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.CollectionInfo

@Composable
internal fun rememberLazyGridSemanticState(
    state: LazyGridState,
    reverseScrolling: Boolean
): LazyLayoutSemanticState =
    remember(state, reverseScrolling) {
        object : LazyLayoutSemanticState {
            override val firstVisibleItemScrollOffset: Int
                get() = state.firstVisibleItemScrollOffset
            override val firstVisibleItemIndex: Int
                get() = state.firstVisibleItemIndex
            override val canScrollForward: Boolean
                get() = state.canScrollForward

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