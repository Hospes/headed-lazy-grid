package ua.hospes.lazygrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider

@ExperimentalFoundationApi
internal interface LazyGridItemProvider : LazyLayoutItemProvider {
    /** The list of indexes of the sticky header items */
    val headerIndexes: List<Int>

    val spanLayoutProvider: LazyGridSpanLayoutProvider
}