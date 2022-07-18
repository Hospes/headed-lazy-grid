package ua.hospes.lazygrid

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset

@OptIn(ExperimentalFoundationApi::class)
internal object LazyGridItemScopeImpl : LazyGridItemScope {
    @ExperimentalFoundationApi
    override fun Modifier.animateItemPlacement(animationSpec: FiniteAnimationSpec<IntOffset>) =
        this.then(AnimateItemPlacementModifier(animationSpec, debugInspectorInfo {
            name = "animateItemPlacement"
            value = animationSpec
        }))
}

private class AnimateItemPlacementModifier(
    val animationSpec: FiniteAnimationSpec<IntOffset>,
    inspectorInfo: InspectorInfo.() -> Unit,
) : ParentDataModifier, InspectorValueInfo(inspectorInfo) {
    override fun Density.modifyParentData(parentData: Any?): Any = animationSpec

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimateItemPlacementModifier) return false
        return animationSpec != other.animationSpec
    }

    override fun hashCode(): Int {
        return animationSpec.hashCode()
    }
}