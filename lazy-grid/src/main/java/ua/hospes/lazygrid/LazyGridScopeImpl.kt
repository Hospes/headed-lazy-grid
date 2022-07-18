package ua.hospes.lazygrid

import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable

internal class LazyGridScopeImpl : LazyGridScope {

    private val _intervals = MutableIntervalList<LazyGridIntervalContent>()
    val intervals: IntervalList<LazyGridIntervalContent> = _intervals
    internal var hasCustomSpans = false

    private var _headerIndexes: MutableList<Int>? = null
    val headerIndexes: List<Int> get() = _headerIndexes ?: emptyList()

    private val DefaultSpan: LazyGridItemSpanScope.(Int) -> GridItemSpan = { GridItemSpan(1) }

    override fun item(
        key: Any?,
        span: (LazyGridItemSpanScope.() -> GridItemSpan)?,
        contentType: Any?,
        content: @Composable LazyGridItemScope.() -> Unit
    ) {
        _intervals.addInterval(
            1,
            LazyGridIntervalContent(
                key = key?.let { { key } },
                span = span?.let { { span() } } ?: DefaultSpan,
                type = { contentType },
                item = { content() }
            )
        )
        if (span != null) hasCustomSpans = true
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        span: (LazyGridItemSpanScope.(Int) -> GridItemSpan)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable LazyGridItemScope.(index: Int) -> Unit
    ) {
        _intervals.addInterval(
            count,
            LazyGridIntervalContent(
                key = key,
                span = span ?: DefaultSpan,
                type = contentType,
                item = itemContent
            )
        )
        if (span != null) hasCustomSpans = true
    }

    override fun stickyHeader(
        key: Any?,
        span: LazyGridItemSpanScope.() -> GridItemSpan,
        contentType: Any?,
        content: @Composable LazyGridItemScope.() -> Unit
    ) {
        val headersIndexes = _headerIndexes ?: mutableListOf<Int>().also { _headerIndexes = it }
        headersIndexes.add(_intervals.size)

        item(key = key, span = span, contentType = contentType, content = content)
    }
}

internal class LazyGridIntervalContent(
    val key: ((index: Int) -> Any)?,
    val span: LazyGridItemSpanScope.(Int) -> GridItemSpan,
    val type: ((index: Int) -> Any?),
    val item: @Composable LazyGridItemScope.(Int) -> Unit
)