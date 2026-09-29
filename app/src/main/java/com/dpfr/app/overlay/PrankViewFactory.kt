package com.dpfr.app.overlay

import android.content.Context
import android.view.View
import com.dpfr.app.data.CustomShape
import com.dpfr.app.overlay.views.BugKind
import com.dpfr.app.overlay.views.BugsView
import com.dpfr.app.overlay.views.CrackedScreenView
import com.dpfr.app.overlay.views.CustomShapeView
import com.dpfr.app.overlay.views.DeadPixelView
import com.dpfr.app.overlay.views.DimmerView
import com.dpfr.app.overlay.views.InkStainView

/** Views that can be made stronger or weaker with the edit bar. */
interface Adjustable {
    var intensity: Float
}

object PrankViewFactory {

    fun create(context: Context, ref: PrankRef, shape: CustomShape?): View = when (ref.type) {
        PrankType.CRACKED_1 -> CrackedScreenView(context, 1)
        PrankType.CRACKED_2 -> CrackedScreenView(context, 2)
        PrankType.CRACKED_3 -> CrackedScreenView(context, 3)
        PrankType.BUG_COCKROACH -> BugsView(context, BugKind.COCKROACH)
        PrankType.BUG_BEETLE -> BugsView(context, BugKind.BEETLE)
        PrankType.BUG_ANT -> BugsView(context, BugKind.ANT)
        PrankType.INK -> InkStainView(context)
        PrankType.DEAD_PIXEL -> DeadPixelView(context)
        PrankType.DIMMER -> DimmerView(context)
        PrankType.CUSTOM -> CustomShapeView(context).also { it.shape = shape }
    }
}
