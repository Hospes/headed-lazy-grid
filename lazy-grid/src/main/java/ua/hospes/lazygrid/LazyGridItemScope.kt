package ua.hospes.lazygrid

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.LazyGridScopeMarker
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

/**
 * Receiver scope being used by the item content parameter of [LazyVerticalGrid].
 */
@Stable
@LazyGridScopeMarker
sealed interface LazyGridItemScope {
    /**
     * This modifier animates the item placement within the Lazy grid.
     *
     * When you provide a key via [LazyGridScope.item]/[LazyGridScope.items] this modifier will
     * enable item reordering animations. Aside from item reordering all other position changes
     * caused by events like arrangement or alignment changes will also be animated.
     *
     * @param animationSpec a finite animation that will be used to animate the item placement.
     */
    @ExperimentalFoundationApi
    fun Modifier.animateItemPlacement(
        animationSpec: FiniteAnimationSpec<IntOffset> = spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )
    ): Modifier
}