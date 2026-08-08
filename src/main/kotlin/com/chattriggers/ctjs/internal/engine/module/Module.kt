package com.chattriggers.ctjs.internal.engine.module

import com.chattriggers.ctjs.api.message.ChatLib
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.api.render.Text
import com.fasterxml.jackson.core.Version
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.io.File

class Module(val name: String, var metadata: ModuleMetadata, val folder: File) {
    var targetModVersion: Version? = null
    var requiredBy = mutableSetOf<String>()

    private val gui = object {
        var collapsed = true
        var x = 0
        var y = 0
        var description = Text(metadata.description ?: "No description provided in the metadata")
    }

    fun draw(ctx: GuiGraphicsExtractor, x: Int, y: Int, width: Int): Int {
        gui.x = x
        gui.y = y

        ctx.pose().pushMatrix()

        ctx.fill(x, y, x + width, y + 13, 0xaa000000.toInt())
        ctx.text(
            Renderer.getFontRenderer(),
            metadata.name ?: name,
            x + 3, y + 3, -1
        )

        return if (gui.collapsed) {
            ctx.pose().pushMatrix()
            ctx.pose().translate(x + width - 5f, y + 8f)
            ctx.pose().rotate(Math.PI.toFloat())
            ctx.text(Renderer.getFontRenderer(), "^", 0, 0, -1, false)
            ctx.pose().popMatrix()
            16
        } else {
            gui.description.setMaxWidth(width - 5)

            ctx.fill(x, y + 13, x + width, y + (gui.description.getHeight().toInt() + 25), 0x50000000)
            ctx.text(Renderer.getFontRenderer(), "^", x + width - 10, y + 5, -1, false)

            gui.description.draw(ctx, x + 3, y + 15)

            if (metadata.version != null) {
                ctx.text(
                    Renderer.getFontRenderer(),
                    ChatLib.addColor("&8v${metadata.version}"),
                    x + width - Renderer.getStringWidth(ChatLib.addColor("&8v${metadata.version}")),
                    y + gui.description.getHeight().toInt() + 15,
                    -1
                )
            }

            ctx.text(
                Renderer.getFontRenderer(),
                ChatLib.addColor(
                    if (metadata.isRequired && requiredBy.isNotEmpty()) {
                        "&8required by $requiredBy"
                    } else {
                        "&4[delete]"
                    }
                ),
                x + 3,
                y + gui.description.getHeight().toInt() + 15,
                -1
            )

            ctx.pose().popMatrix()
            gui.description.getHeight().toInt() + 27
        }
    }

    fun click(x: Double, y: Double, width: Float) {
        if (x > gui.x && x < gui.x + width
            && y > gui.y && y < gui.y + 13
        ) {
            gui.collapsed = !gui.collapsed
            return
        }

        if (gui.collapsed || (metadata.isRequired && requiredBy.isNotEmpty())) return

        if (x > gui.x && x < gui.x + 45
            && y > gui.y + gui.description.getHeight() + 15 && y < gui.y + gui.description.getHeight() + 25
        ) {
            ModuleManager.deleteModule(name)
        }
    }

    override fun toString() = "Module{name=$name,version=${metadata.version}}"
}
