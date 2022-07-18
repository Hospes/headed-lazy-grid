package ua.hospes.lazygrid

/**
 * Represents a line index in the lazy grid.
 */
@Suppress("NOTHING_TO_INLINE")
@kotlin.jvm.JvmInline
internal value class LineIndex(val value: Int) {
    inline operator fun inc(): LineIndex = LineIndex(value + 1)
    inline operator fun dec(): LineIndex = LineIndex(value - 1)
    inline operator fun plus(i: Int): LineIndex = LineIndex(value + i)
    inline operator fun minus(i: Int): LineIndex = LineIndex(value - i)
    inline operator fun minus(i: LineIndex): LineIndex = LineIndex(value - i.value)
    inline operator fun compareTo(other: LineIndex): Int = value - other.value
}

/**
 * Represents an item index in the lazy grid.
 */
@Suppress("NOTHING_TO_INLINE")
@kotlin.jvm.JvmInline
internal value class ItemIndex(val value: Int) {
    inline operator fun inc(): ItemIndex = ItemIndex(value + 1)
    inline operator fun dec(): ItemIndex = ItemIndex(value - 1)
    inline operator fun plus(i: Int): ItemIndex = ItemIndex(value + i)
    inline operator fun minus(i: Int): ItemIndex = ItemIndex(value - i)
    inline operator fun minus(i: ItemIndex): ItemIndex = ItemIndex(value - i.value)
    inline operator fun compareTo(other: ItemIndex): Int = value - other.value
}