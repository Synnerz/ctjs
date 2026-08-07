package com.chattriggers.ctjs.internal.listeners

import com.chattriggers.ctjs.MCBlockPos
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.api.triggers.CancellableEvent
import com.chattriggers.ctjs.api.triggers.TriggerType
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.core.BlockPos

object WorldListener {
    var matrixStack: PoseStack? = null
    private var deltaTicks: Float = 1f

    fun triggerBlockOutline(bp: MCBlockPos): Boolean {
        val event = CancellableEvent()
        TriggerType.BLOCK_HIGHLIGHT.triggerAll(BlockPos(bp), event)
        return event.isCanceled()
    }

    fun triggerRenderStart(ticks: Float) {
        deltaTicks = ticks
        if (matrixStack == null) return
        Renderer.withMatrix(matrixStack, ticks) {
            TriggerType.PRE_RENDER_WORLD.triggerAll(ticks)
        }
    }

    fun triggerRenderLast() {
        if (matrixStack == null) return
        Renderer.withMatrix(matrixStack, deltaTicks) {
            TriggerType.POST_RENDER_WORLD.triggerAll(deltaTicks)
        }
    }
}
