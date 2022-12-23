package ua.hospes.lazygrid

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import kotlinx.coroutines.launch

@Suppress("ComposableModifierFactory", "ModifierInspectorInfo")
@Composable
internal fun Modifier.lazyLayoutSemantics(
    itemProvider: LazyLayoutItemProvider,
    state: LazyLayoutSemanticState,
    orientation: Orientation,
    userScrollEnabled: Boolean
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    return this.then(
        remember(
            itemProvider,
            state,
            orientation,
            userScrollEnabled
        ) {
            val isVertical = orientation == Orientation.Vertical
            val indexForKeyMapping: (Any) -> Int = { needle ->
                var result = -1
                for (index in 0 until itemProvider.itemCount) {
                    if (itemProvider.getKey(index) == needle) {
                        result = index
                        break
                    }
                }
                result
            }

            val accessibilityScrollState = state.scrollAxisRange()

            val scrollByAction: ((x: Float, y: Float) -> Boolean)? = if (userScrollEnabled) {
                { x, y ->
                    val delta = if (isVertical) {
                        y
                    } else {
                        x
                    }
                    coroutineScope.launch {
                        state.animateScrollBy(delta)
                    }
                    // TODO(aelias): is it important to return false if we know in advance we cannot scroll?
                    true
                }
            } else {
                null
            }

            val scrollToIndexAction: ((Int) -> Boolean)? = if (userScrollEnabled) {
                { index ->
                    require(index >= 0 && index < itemProvider.itemCount) {
                        "Can't scroll to index $index, it is out of " +
                                "bounds [0, ${itemProvider.itemCount})"
                    }
                    coroutineScope.launch {
                        state.scrollToItem(index)
                    }
                    true
                }
            } else {
                null
            }

            val collectionInfo = state.collectionInfo()

            Modifier.semantics {
                indexForKey(indexForKeyMapping)

                if (isVertical) {
                    verticalScrollAxisRange = accessibilityScrollState
                } else {
                    horizontalScrollAxisRange = accessibilityScrollState
                }

                if (scrollByAction != null) {
                    scrollBy(action = scrollByAction)
                }

                if (scrollToIndexAction != null) {
                    scrollToIndex(action = scrollToIndexAction)
                }

                this.collectionInfo = collectionInfo
            }
        }
    )
}

internal interface LazyLayoutSemanticState {
    fun scrollAxisRange(): ScrollAxisRange
    fun collectionInfo(): CollectionInfo
    suspend fun animateScrollBy(delta: Float)
    suspend fun scrollToItem(index: Int)
}