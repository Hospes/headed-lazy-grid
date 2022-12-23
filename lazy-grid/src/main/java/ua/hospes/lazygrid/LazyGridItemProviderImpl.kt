package ua.hospes.lazygrid

import androidx.compose.runtime.*

//@ExperimentalFoundationApi
//internal class LazyGridItemsSnapshot(
//    private val intervals: IntervalList<LazyGridIntervalContent>,
//    val headerIndexes: List<Int>,
//    val hasCustomSpans: Boolean,
//    nearestItemsRange: IntRange
//) {
//    val itemsCount get() = intervals.size
//
//    val spanLayoutProvider = LazyGridSpanLayoutProvider(this)
//
//    fun getKey(index: Int): Any {
//        val interval = intervals[index]
//        val localIntervalIndex = index - interval.startIndex
//        val key = interval.value.key?.invoke(localIntervalIndex)
//        return key ?: getDefaultLazyLayoutKey(index)
//    }
//
//    fun LazyGridItemSpanScope.getSpan(index: Int): GridItemSpan {
//        val interval = intervals[index]
//        val localIntervalIndex = index - interval.startIndex
//        return interval.value.span.invoke(this, localIntervalIndex)
//    }
//
//    @Composable
//    fun Item(index: Int) {
//        val interval = intervals[index]
//        val localIntervalIndex = index - interval.startIndex
//        interval.value.item.invoke(LazyGridItemScopeImpl, localIntervalIndex)
//    }
//
//    val keyToIndexMap: Map<Any, Int> = generateKeyToIndexMap(nearestItemsRange, intervals)
//
//    fun getContentType(index: Int): Any? {
//        val interval = intervals[index]
//        val localIntervalIndex = index - interval.startIndex
//        return interval.value.type.invoke(localIntervalIndex)
//    }
//}

//@ExperimentalFoundationApi
//internal class LazyGridItemProviderImpl(
//    private val itemsSnapshot: State<LazyGridItemsSnapshot>
//) : LazyGridItemProvider {
//
//    override val headerIndexes: List<Int> get() = itemsSnapshot.value.headerIndexes
//
//    override val itemCount get() = itemsSnapshot.value.itemsCount
//
//    override fun getKey(index: Int) = itemsSnapshot.value.getKey(index)
//
//    @Composable
//    override fun Item(index: Int) {
//        itemsSnapshot.value.Item(index)
//    }
//
//    override val keyToIndexMap: Map<Any, Int> get() = itemsSnapshot.value.keyToIndexMap
//
//    override fun getContentType(index: Int) = itemsSnapshot.value.getContentType(index)
//
//    override val spanLayoutProvider: LazyGridSpanLayoutProvider
//        get() = itemsSnapshot.value.spanLayoutProvider
//}

/**
 * Traverses the interval [list] in order to create a mapping from the key to the index for all
 * the indexes in the passed [range].
 * The returned map will not contain the values for intervals with no key mapping provided.
 */
//@ExperimentalFoundationApi
//internal fun generateKeyToIndexMap(
//    range: IntRange,
//    list: IntervalList<LazyGridIntervalContent>
//): Map<Any, Int> {
//    val first = range.first
//    check(first >= 0)
//    val last = minOf(range.last, list.size - 1)
//    return if (last < first) {
//        emptyMap()
//    } else {
//        hashMapOf<Any, Int>().also { map ->
//            list.forEach(
//                fromIndex = first,
//                toIndex = last,
//            ) {
//                if (it.value.key != null) {
//                    val keyFactory = requireNotNull(it.value.key)
//                    val start = maxOf(first, it.startIndex)
//                    val end = minOf(last, it.startIndex + it.size - 1)
//                    for (i in start..end) {
//                        map[keyFactory(i - it.startIndex)] = i
//                    }
//                }
//            }
//        }
//    }
//}