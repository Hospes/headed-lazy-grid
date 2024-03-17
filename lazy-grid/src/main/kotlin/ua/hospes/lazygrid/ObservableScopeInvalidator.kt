package ua.hospes.lazygrid

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy

/**
 * Simple wrapper over a mutable state which allows to invalidate an observable scope.
 * We might consider providing something like this in the public api in the future.
 */
@JvmInline
internal value class ObservableScopeInvalidator(
    private val state: MutableState<Unit> = mutableStateOf(Unit, neverEqualPolicy())
) {
    fun attachToScope() {
        state.value
    }

    fun invalidateScope() {
        state.value = Unit
    }
}