package ua.hospes.lazygrid

import androidx.compose.foundation.gestures.Orientation


internal fun LazyGridItemInfo.offsetOnMainAxis(orientation: Orientation): Int {
    return if (orientation == Orientation.Vertical) {
        offset.y
    } else {
        offset.x
    }
}