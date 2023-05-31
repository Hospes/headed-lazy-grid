package ua.hospes.lazygrid

import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints

/**
 * Abstracts away the subcomposition from the measuring logic.
 */
internal class LazyGridMeasuredItemProvider constructor(
    private val itemProvider: LazyGridItemProvider,
    private val measureScope: LazyLayoutMeasureScope,
    private val defaultMainAxisSpacing: Int,
    private val measuredItemFactory: MeasuredItemFactory
) {
    /**
     * Used to subcompose individual items of lazy grids. Composed placeables will be measured
     * with the provided [constraints] and wrapped into [LazyGridMeasuredItem].
     */
    fun getAndMeasure(
        index: Int,
        mainAxisSpacing: Int = defaultMainAxisSpacing,
        constraints: Constraints
    ): LazyGridMeasuredItem {
        val key = keyIndexMap.getKey(index) ?: itemProvider.getKey(index)
        val contentType = itemProvider.getContentType(index)
        val placeables = measureScope.measure(index, constraints)
        val crossAxisSize = if (constraints.hasFixedWidth) {
            constraints.minWidth
        } else {
            require(constraints.hasFixedHeight)
            constraints.minHeight
        }
        return measuredItemFactory.createItem(
            index,
            key,
            contentType,
            crossAxisSize,
            mainAxisSpacing,
            placeables
        )
    }

    /**
     * Contains the mapping between the key and the index. It could contain not all the items of
     * the list as an optimization.
     **/
    val keyIndexMap: LazyLayoutKeyIndexMap = itemProvider.keyIndexMap
}

// This interface allows to avoid autoboxing on index param
internal fun interface MeasuredItemFactory {
    fun createItem(
        index: Int,
        key: Any,
        contentType: Any?,
        crossAxisSize: Int,
        mainAxisSpacing: Int,
        placeables: List<Placeable>
    ): LazyGridMeasuredItem
}