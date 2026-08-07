package com.chattriggers.ctjs.internal.engine.module

import com.chattriggers.ctjs.api.client.Player
import com.chattriggers.ctjs.api.message.ChatLib
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.api.render.Text
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen

object ModulesGui : Screen(net.minecraft.network.chat.Component.literal("Modules")) {
    private val window = object {
        val title = Text("Modules").setScale(2f).setShadow(true)
        val exit = Text(ChatLib.addColor("&cx")).setScale(2f)
        var height = 0f
        var scroll = 0f
    }

    override fun render(ctx: GuiGraphics?, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        ctx!!.pose().pushMatrix()

        val middle = Renderer.screen.getWidth() / 2
        val width = (Renderer.screen.getWidth() - 100).coerceAtMost(500)

        ctx.fill(0, 0, ctx.guiWidth(), ctx.guiHeight(), 0x50000000)

        if (-window.scroll > window.height - Renderer.screen.getHeight() + 20)
            window.scroll = -window.height + Renderer.screen.getHeight() - 20
        if (-window.scroll < 0) window.scroll = 0f

        if (-window.scroll > 0) {
            val rx = Renderer.screen.getWidth() - 20
            val ry = Renderer.screen.getHeight() - 20
            ctx.fill(rx, ry, rx + 20, ry + 20, 0xaa000000.toInt())
            ctx.drawString(Renderer.getFontRenderer(), "^", Renderer.screen.getWidth() - 12, Renderer.screen.getHeight() - 12, -1, false)
        }

        val ox = middle - width / 2
        val oy = window.scroll.toInt() + 95

        ctx.fill(ox, oy, ox + width, oy + (window.height.toInt() - 90), 0x50000000)
        ctx.fill(ox, oy, ox + width, oy + 25, 0xaa000000.toInt())

        window.title.draw(ctx, (middle - width / 2 + 5) / 2, (window.scroll.toInt() + 100) / 2)
        window.exit.draw(ctx, (middle + width / 2 - 17) / 2, (window.scroll.toInt() + 99) / 2)

        window.height = 125f
        ModuleManager.cachedModules.sortedBy { it.name }.forEach {
            window.height += it.draw(ctx, middle - width / 2, (window.scroll + window.height).toInt(), width)
        }

        ctx.pose().popMatrix()
    }

    override fun mouseClicked(click: MouseButtonEvent, double: Boolean): Boolean {
        super.mouseClicked(click, double)
        val mouseX = click.x
        val mouseY = click.y

        var width = Renderer.screen.getWidth() - 100f
        if (width > 500) width = 500f

        if (mouseX > Renderer.screen.getWidth() - 20 && mouseY > Renderer.screen.getHeight() - 20) {
            window.scroll = 0f
            return false
        }

        if (mouseX > Renderer.screen.getWidth() / 2f + width / 2f - 25 && mouseX < Renderer.screen.getWidth() / 2f + width / 2f
            && mouseY > window.scroll + 95 && mouseY < window.scroll + 120
        ) {
            Player.toMC()?.clientSideCloseContainer()
            return false
        }

        ModuleManager.cachedModules.toList().forEach {
            it.click(mouseX, mouseY, width)
        }

        return false
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        delta: Double
    ): Boolean {
        super.mouseScrolled(mouseX, mouseY, horizontalAmount, delta)
        window.scroll += delta.toFloat()
        return false
    }
}
