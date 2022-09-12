package ua.hospes.lazygrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints

/**
 * Abstracts away the subcomposition from the measuring logic.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class LazyMeasuredItemProvider @ExperimentalFoundationApi constructor(
    private val itemProvider: LazyGridItemProvider,
    private val measureScope: LazyLayoutMeasureScope,
    private val defaultMainAxisSpacing: Int,
    private val measuredItemFactory: MeasuredItemFactory
) {
    /**
     * Used to subcompose individual items of lazy grids. Composed placeables will be measured
     * with the provided [constraints] and wrapped into [LazyMeasuredItem].
     */
    fun getAndMeasure(
        index: ItemIndex,
        mainAxisSpacing: Int = defaultMainAxisSpacing,
        constraints: Constraints
    ): LazyMeasuredItem {
        val key = itemProvider.getKey(index.value)
        val placeables = measureScope.measure(index.value, constraints)
        val crossAxisSize = if (constraints.hasFixedWidth) {
            constraints.minWidth
        } else {
            require(constraints.hasFixedHeight)
            constraints.minHeight
        }
        return measuredItemFactory.createItem(
            index,
            key,
            crossAxisSize,
            mainAxisSpacing,
            placeables,
        )
    }

    /**
     * Contains the mapping between the key and the index. It could contain not all the items of
     * the list as an optimization.
     **/
    val keyToIndexMap: Map<Any, Int> get() = itemProvider.keyToIndexMap
}

// This interface allows to avoid autoboxing on index param
internal fun interface MeasuredItemFactory {
    fun createItem(
        index: ItemIndex,
        key: Any,
        crossAxisSize: Int,
        mainAxisSpacing: Int,
        placeables: List<Placeable>
    ): LazyMeasuredItem
}