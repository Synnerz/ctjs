package com.chattriggers.ctjs.api.render

import com.chattriggers.ctjs.MCVertexFormat
import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.client.Player
import com.chattriggers.ctjs.api.entity.PlayerMP
import com.chattriggers.ctjs.api.message.ChatLib
import com.chattriggers.ctjs.api.vec.Vec3f
import com.chattriggers.ctjs.engine.LogType
import com.chattriggers.ctjs.engine.printToConsole
import com.chattriggers.ctjs.internal.mixins.GameRendererAccessor
import com.chattriggers.ctjs.internal.mixins.LevelRendererAccessor
import com.chattriggers.ctjs.internal.utils.getOrDefault
import com.chattriggers.ctjs.internal.utils.toRadians
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet
import gg.essential.elementa.dsl.component1
import gg.essential.elementa.dsl.component2
import gg.essential.elementa.dsl.component3
import gg.essential.elementa.dsl.component4
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.player.AbstractClientPlayer
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.entity.EntityRendererProvider
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.mozilla.javascript.NativeObject
import java.awt.Color
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.*

object Renderer {
    private val NEWLINE_REGEX = """\n|\r\n?""".toRegex()

    @JvmField
    var colorized: Long? = null

    // The currently-active matrix stack
    internal lateinit var matrixStack: UMatrixStack
    private val matrixStackStack = ArrayDeque<UMatrixStack>()

    private lateinit var slimCTRenderPlayer: CTPlayerRenderer
    private lateinit var normalCTRenderPlayer: CTPlayerRenderer

    internal var matrixPushCounter = 0

    @JvmField
    val screen = ScreenWrapper()

    // The current partialTicks value
    @JvmStatic
    var partialTicks = 0f
        internal set

    @JvmField
    val BLACK = getColor(0, 0, 0, 255)

    @JvmField
    val DARK_BLUE = getColor(0, 0, 190, 255)

    @JvmField
    val DARK_GREEN = getColor(0, 190, 0, 255)

    @JvmField
    val DARK_AQUA = getColor(0, 190, 190, 255)

    @JvmField
    val DARK_RED = getColor(190, 0, 0, 255)

    @JvmField
    val DARK_PURPLE = getColor(190, 0, 190, 255)

    @JvmField
    val GOLD = getColor(217, 163, 52, 255)

    @JvmField
    val GRAY = getColor(190, 190, 190, 255)

    @JvmField
    val DARK_GRAY = getColor(63, 63, 63, 255)

    @JvmField
    val BLUE = getColor(63, 63, 254, 255)

    @JvmField
    val GREEN = getColor(63, 254, 63, 255)

    @JvmField
    val AQUA = getColor(63, 254, 254, 255)

    @JvmField
    val RED = getColor(254, 63, 63, 255)

    @JvmField
    val LIGHT_PURPLE = getColor(254, 63, 254, 255)

    @JvmField
    val YELLOW = getColor(254, 254, 63, 255)

    @JvmField
    val WHITE = getColor(255, 255, 255, 255)

    @JvmStatic
    fun color(color: Int): Long {
        return when (color) {
            0 -> BLACK
            1 -> DARK_BLUE
            2 -> DARK_GREEN
            3 -> DARK_AQUA
            4 -> DARK_RED
            5 -> DARK_PURPLE
            6 -> GOLD
            7 -> GRAY
            8 -> DARK_GRAY
            9 -> BLUE
            10 -> GREEN
            11 -> AQUA
            12 -> RED
            13 -> LIGHT_PURPLE
            14 -> YELLOW
            else -> WHITE
        }
    }

    @JvmStatic
    internal fun initializePlayerRenderers(context: EntityRendererProvider.Context) {
        normalCTRenderPlayer = CTPlayerRenderer(context, slim = false)
        slimCTRenderPlayer = CTPlayerRenderer(context, slim = true)
    }

    @JvmStatic
    fun getFontRenderer() = Client.getMinecraft().font

    @JvmStatic
    fun getRenderManager() = Client.getMinecraft().levelRenderer

    @JvmStatic
    fun getStringWidth(text: String) = getFontRenderer().width(ChatLib.addColor(text))

    @JvmStatic
    @JvmOverloads
    fun getColor(red: Int, green: Int, blue: Int, alpha: Int = 255): Long {
        return ((alpha.coerceIn(0, 255) shl 24) or
                (red.coerceIn(0, 255) shl 16) or
                (green.coerceIn(0, 255) shl 8) or
                blue.coerceIn(0, 255)).toLong()
    }

    @JvmStatic
    @JvmOverloads
    fun getRainbow(step: Float, speed: Float = 1f): Long {
        val red = ((sin(step / speed) + 0.75) * 170).toInt()
        val green = ((sin(step / speed + 2 * PI / 3) + 0.75) * 170).toInt()
        val blue = ((sin(step / speed + 4 * PI / 3) + 0.75) * 170).toInt()
        return getColor(red, green, blue)
    }

    @JvmStatic
    @JvmOverloads
    fun getRainbowColors(step: Float, speed: Float = 1f): IntArray {
        val red = ((sin(step / speed) + 0.75) * 170).toInt()
        val green = ((sin(step / speed + 2 * PI / 3) + 0.75) * 170).toInt()
        val blue = ((sin(step / speed + 4 * PI / 3) + 0.75) * 170).toInt()
        return intArrayOf(red, green, blue)
    }

    @JvmStatic
    fun disableCull() = apply { LegacyPipelineBuilder.disableCull() }

    @JvmStatic
    fun enableCull() = apply { LegacyPipelineBuilder.enableCull() }

    @JvmStatic
    fun disableLighting() = apply { UGraphics.disableLighting() }

    @JvmStatic
    fun enableLighting() = apply { UGraphics.enableLighting() }

    @JvmStatic
    fun disableDepth() = apply { LegacyPipelineBuilder.disableDepth() }

    @JvmStatic
    fun enableDepth() = apply { LegacyPipelineBuilder.enableDepth() }

    @JvmStatic
    fun depthFunc(func: Int) = apply { UGraphics.depthFunc(func) }

    @JvmStatic
    fun depthMask(flag: Boolean) = apply { UGraphics.depthMask(flag) }

    @JvmStatic
    fun disableBlend() = apply { LegacyPipelineBuilder.disableBlend() }

    @JvmStatic
    fun enableBlend() = apply { LegacyPipelineBuilder.enabledBlend() }

    @JvmStatic
    fun blendFunc(func: Int) = apply { UGraphics.blendEquation(func) }

    @JvmStatic
    fun tryBlendFuncSeparate(
        sourceFactor: Int,
        destFactor: Int,
        sourceFactorAlpha: Int,
        destFactorAlpha: Int,
    ) = apply {
        UGraphics.tryBlendFuncSeparate(sourceFactor, destFactor, sourceFactorAlpha, destFactorAlpha)
    }

    @JvmStatic
    @JvmOverloads
    fun bindTexture(texture: Image, textureIndex: Int = 0) = apply {
        UGraphics.bindTexture(textureIndex, texture.getTexture()?.pixels?.pointer?.toInt() ?: 0)
    }

    @JvmStatic
    fun deleteTexture(texture: Image) = apply {
        UGraphics.deleteTexture(texture.getTexture()?.pixels?.pointer?.toInt() ?: 0)
    }

    @JvmStatic
    @JvmOverloads
    fun pushMatrix(stack: UMatrixStack = matrixStack) = apply {
        matrixPushCounter++
        matrixStackStack.addLast(stack)
        matrixStack = stack
        stack.push()
    }

    @JvmStatic
    fun popMatrix() = apply {
        matrixPushCounter--
        matrixStackStack.removeLast()
        matrixStack.pop()
    }

    @JvmStatic
    @JvmOverloads
    fun translate(x: Float, y: Float, z: Float = 0.0F) = apply {
        matrixStack.translate(x, y, z)
    }

    @JvmStatic
    @JvmOverloads
    fun scale(scaleX: Float, scaleY: Float = scaleX, scaleZ: Float = 1f) = apply {
        matrixStack.scale(scaleX, scaleY, scaleZ)
    }

    @JvmStatic
    @JvmOverloads
    fun rotate(angle: Float, x: Float = 0f, y: Float = 0f, z: Float = 1f) = apply {
        matrixStack.rotate(angle, x, y, z)
    }

    @JvmStatic
    fun multiply(quaternion: Quaternionf) = apply {
        matrixStack.multiply(quaternion)
    }

    @JvmStatic
    @JvmOverloads
    fun colorize(red: Float, green: Float, blue: Float, alpha: Float = 1f) =
        colorize(
            (red * 255).toInt(),
            (green * 255).toInt(),
            (blue * 255).toInt(),
            (alpha * 255).toInt()
        )

    @JvmStatic
    @JvmOverloads
    @Deprecated("There is no longer an easy way to change the color globally, you need to find out each color you do.")
    fun colorize(red: Int, green: Int, blue: Int, alpha: Int = 255) = apply {}

    @JvmStatic
    fun fixAlpha(color: Long): Long {
        val alpha = color ushr 24 and 255
        return if (alpha < 10)
            (color and 0xFF_FF_FF) or 0xA_FF_FF_FF
        else color
    }

    /**
     * Begin drawing with the world renderer
     *
     * @param drawMode the GL draw mode
     * @param vertexFormat The [VertexFormat] to use for drawing
     * @return [Renderer] to allow for method chaining
     * @see DrawMode
     */
    @JvmStatic
    @JvmOverloads
    fun begin(
        drawMode: DrawMode = Renderer.DrawMode.QUADS,
        vertexFormat: VertexFormat = Renderer.VertexFormat.POSITION,
        snippet: Renderer.RenderSnippet = Renderer.RenderSnippet.POSITION_COLOR_SNIPPET
    ) = apply {
        Renderer3d.begin(drawMode, vertexFormat, snippet)
    }

    /**
     * Sets a new vertex in the world renderer.
     *
     * @param x the x position
     * @param y the y position
     * @param z the z position
     * @return [Renderer] to allow for method chaining
     */
    @JvmStatic
    @JvmOverloads
    fun pos(x: Float, y: Float, z: Float = 0f) = apply {
        val camera = Client.getMinecraft().gameRenderer.mainCamera().position()
        Renderer3d.pos(x + camera.x.toFloat(), y + camera.y.toFloat(), z + camera.z.toFloat())
    }

    /**
     * Sets the texture location on the last defined vertex.
     *
     * @param u the u position in the texture
     * @param v the v position in the texture
     * @return [Renderer] to allow for method chaining
     */
    @JvmStatic
    fun tex(u: Float, v: Float) = apply {
        Renderer3d.tex(u, v)
    }

    /**
     * Sets the color for the last defined vertex.
     *
     * @param r the red value of the color, between 0 and 1
     * @param g the green value of the color, between 0 and 1
     * @param b the blue value of the color, between 0 and 1
     * @param a the alpha value of the color, between 0 and 1
     * @return [Renderer] to allow for method chaining
     */
    @JvmStatic
    @JvmOverloads
    fun color(r: Float, g: Float, b: Float, a: Float = 1f) = apply {
        Renderer3d.color(r, g, b, a)
    }

    /**
     * Sets the color for the last defined vertex.
     *
     * @param r the red value of the color, between 0 and 255
     * @param g the green value of the color, between 0 and 255
     * @param b the blue value of the color, between 0 and 255
     * @param a the alpha value of the color, between 0 and 255
     * @return [Renderer] to allow for method chaining
     */
    @JvmStatic
    @JvmOverloads
    fun color(r: Int, g: Int, b: Int, a: Int = 255) = apply {
        Renderer3d.color(r, g, b, a)
    }

    /**
     * Sets the color for the last defined vertex.
     *
     * @param color the color value, can use [getColor] to get this
     * @return [Renderer] to allow for method chaining
     */
    @JvmStatic
    fun color(color: Long) = apply {
        val (r, g, b, a) = Color(color.toInt(), true)
        Renderer3d.color(r, g, b, a)
    }

    /**
     * Sets the normal of the vertex. This is mostly used with [VertexFormat.LINES]
     *
     * @param x the x position of the normal vector
     * @param y the y position of the normal vector
     * @param z the z position of the normal vector
     * @return [Renderer] to allow for method chaining
     */
    @JvmStatic
    fun normal(x: Float, y: Float, z: Float) = apply {
        Renderer3d.normal(x, y, z)
    }

    /**
     * Sets the overlay location on the last defined vertex.
     *
     * @param u the u position in the overlay
     * @param v the v position in the overlay
     * @return [Renderer] to allow for method chaining
     */
    @JvmStatic
    fun overlay(u: Int, v: Int) = apply {
        Renderer3d.overlay(u, v)
    }

    /**
     * Sets the light location on the last defined vertex.
     *
     * @param u the u position in the light
     * @param v the v position in the light
     * @return [Renderer] to allow for method chaining
     */
    @JvmStatic
    fun light(u: Int, v: Int) = apply {
        Renderer3d.light(u, v)
    }

    /**
     * Sets the line width when rendering [DrawMode.LINES]
     *
     * @param width the width of the line
     * @return [Renderer] to allow for method chaining
     */
    @JvmStatic
    fun lineWidth(width: Float) = apply {
        Renderer3d.lineWidth(width)
    }

    /**
     * Finalizes vertices and draws the world renderer.
     */
    @JvmStatic
    fun draw() = Renderer3d.draw()

    /**
     * Gets a fixed render position from x, y, and z inputs adjusted with partial ticks
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the Vec3f position to render at
     */
    @JvmStatic
    fun getRenderPos(x: Float, y: Float, z: Float): Vec3f {
        return Vec3f(
            x - Player.getRenderX().toFloat(),
            y - Player.getRenderY().toFloat(),
            z - Player.getRenderZ().toFloat()
        )
    }

    @JvmStatic
    fun drawRect(color: Long, x: Float, y: Float, width: Float, height: Float) = apply {
        val pos = mutableListOf(x, y, x + width, y + height)
        if (pos[0] > pos[2])
            Collections.swap(pos, 0, 2)
        if (pos[1] > pos[3])
            Collections.swap(pos, 1, 3)

        begin(vertexFormat = VertexFormat.POSITION_COLOR)
        pos(pos[0], pos[3], 0f).color(color)
        pos(pos[2], pos[3], 0f).color(color)
        pos(pos[2], pos[1], 0f).color(color)
        pos(pos[0], pos[1], 0f).color(color)
        draw()
    }

    @JvmStatic
    fun drawLine(
        color: Long,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        thickness: Float,
    ) {
        val theta = -atan2(y2 - y1, x2 - x1)
        val i = sin(theta) * (thickness / 2)
        val j = cos(theta) * (thickness / 2)

        begin(vertexFormat = VertexFormat.POSITION_COLOR)
        pos(x1 + i, y1 + j, 0f).color(color)
        pos(x2 + i, y2 + j, 0f).color(color)
        pos(x2 - i, y2 - j, 0f).color(color)
        pos(x1 - i, y1 - j, 0f).color(color)
        draw()
    }

    @JvmStatic
    fun drawCircle(
        color: Long,
        x: Float,
        y: Float,
        radius: Float,
        steps: Int,
    ) {
        val theta = 2 * PI / steps
        val cos = cos(theta).toFloat()
        val sin = sin(theta).toFloat()

        var xHolder: Float
        var circleX = 1f
        var circleY = 0f

        begin(DrawMode.TRIANGLE_STRIP, VertexFormat.POSITION_COLOR)

        for (i in 0..steps) {
            pos(x, y, 0f).color(color)
            pos(circleX * radius + x, circleY * radius + y, 0f).color(color)
            xHolder = circleX
            circleX = cos * circleX - sin * circleY
            circleY = sin * xHolder + cos * circleY

            pos(circleX * radius + x, circleY * radius + y, 0f).color(color)
        }

        draw()
    }

    @JvmStatic
    @JvmOverloads
    fun drawString(
        ctx: GuiGraphicsExtractor,
        text: String,
        x: Float,
        y: Float,
        color: Long = colorized ?: WHITE,
        shadow: Boolean = false,
    ) {
        val fr = getFontRenderer()
        var newY = y

        splitText(text).lines.forEach {
            val pose = ctx.pose()

            pose.pushMatrix()
            pose.translate(x, newY)

            ctx.text(
                fr,
                it,
                0,
                0,
                color.toInt(),
                shadow
            )

            pose.popMatrix()
            newY += fr.lineHeight
        }
    }

    @JvmStatic
    @JvmOverloads
    fun drawStringWithShadow(ctx: GuiGraphicsExtractor, text: String, x: Float, y: Float, color: Long = colorized ?: WHITE) =
        drawString(ctx, text, x, y, color, shadow = true)

    internal data class TextLines(val lines: List<String>, val width: Float, val height: Float)

    internal fun splitText(text: String): TextLines {
        val lines = ChatLib.addColor(text).split(NEWLINE_REGEX)
        return TextLines(
            lines,
            lines.maxOf { getFontRenderer().width(it) }.toFloat(),
            (getFontRenderer().lineHeight * lines.size + (lines.size - 1)).toFloat(),
        )
    }

    @JvmStatic
    fun drawImage(image: Image, x: Float, y: Float, width: Float, height: Float) {
        if (colorized == null)
            colorize(1f, 1f, 1f, 1f)

        scale(1f, 1f, 50f)

//        RenderSystem.setShaderTexture(0, image.getTexture()?.textureView)

        begin(DrawMode.QUADS, VertexFormat.POSITION_TEXTURE_COLOR, snippet = RenderSnippet.POSITION_TEX_COLOR_SNIPPET)
        pos(x, y + height, 0f).tex(0f, 1f).color(colorized!!)
        pos(x + width, y + height, 0f).tex(1f, 1f).color(colorized!!)
        pos(x + width, y, 0f).tex(1f, 0f).color(colorized!!)
        pos(x, y, 0f).tex(0f, 0f).color(colorized!!)
        draw()
    }

    /**
     * Draws a player entity to the screen, similar to the one displayed in the inventory screen.
     *
     * Takes a parameter with the following options:
     * - player: The player entity to draw. Can be a [PlayerMP] or [AbstractClientPlayerEntity].
     *           Defaults to Player.toMC()
     * - x: The x position on the screen to render the player
     * - y: The y position on the screen to render the player
     * - size: The size of the rendered player
     * - rotate: Whether the player should look at the mouse cursor, similar to the inventory screen
     * - pitch: THe pitch the rendered player will face, if rotate is false
     * - yaw: The yaw the rendered player will face, if rotate is false
     * - showNametag: Whether the nametag of the player should be rendered
     * - showArmor: Whether the armor of the player should be rendered
     * - showCape: Whether the cape of the player should be rendered
     * - showHeldItem: Whether the held item of the player should be rendered
     * - showArrows: Whether any arrows stuck in the player's model should be rendered
     * - showElytra: Whether the player's Elytra should be rendered
     * - showParrot: Whether a perched parrot should be rendered
     * - showBeeStinger: Whether any stuck bee stingers should be rendered
     *
     * @param obj An options bag
     */
    @JvmStatic
    fun drawPlayer(obj: NativeObject) {
        val entity = obj["player"].let {
            it as? AbstractClientPlayer
                ?: ((it as? PlayerMP)?.toMC() as? AbstractClientPlayer)
                ?: Player.toMC()
                ?: return
        }

        val x = obj.getOrDefault<Number>("x", 0).toInt()
        val y = obj.getOrDefault<Number>("y", 0).toInt()
        val size = obj.getOrDefault<Number>("size", 20).toDouble()
        val rotate = obj.getOrDefault<Boolean>("rotate", false)
        val pitch = obj.getOrDefault<Number>("pitch", 0f).toFloat()
        val yaw = obj.getOrDefault<Number>("yaw", 0f).toFloat()
        val slim = obj.getOrDefault<Boolean>("slim", false)
        val showNametag = obj.getOrDefault<Boolean>("showNametag", false)
        val showArmor = obj.getOrDefault<Boolean>("showArmor", false)
        val showCape = obj.getOrDefault<Boolean>("showCape", false)
        val showHeldItem = obj.getOrDefault<Boolean>("showHeldItem", false)
        val showArrows = obj.getOrDefault<Boolean>("showArrows", false)
        val showElytra = obj.getOrDefault<Boolean>("showElytra", false)
        val showParrot = obj.getOrDefault<Boolean>("showParrot", false)
        val showStingers = obj.getOrDefault<Boolean>("showBeeStinger", false)

        matrixStack.push()

        val (entityYaw, entityPitch) = if (rotate) {
            val mouseX = x - Client.getMouseX()
            val mouseY = y - Client.getMouseY() - (entity.eyeHeight * size)
            atan((mouseX / 40.0f)).toFloat() to atan((mouseY / 40.0f)).toFloat()
        } else {
            val scaleFactor = 130f / 180f
            (yaw * scaleFactor).toRadians() to pitch.toRadians()
        }

        val flipModelRotation = Quaternionf().rotateZ(Math.PI.toFloat())
        val pitchModelRotation =
            Quaternionf().rotateX(entityPitch * 20.0f * (Math.PI / 180.0).toFloat())
        flipModelRotation.mul(pitchModelRotation)

        val oldBodyYaw = entity.yBodyRot
        val oldYaw = entity.yRot
        val oldPitch = entity.xRot
        val oldPrevHeadYaw = entity.yHeadRotO
        val oldHeadYaw = entity.yHeadRot

        entity.yBodyRot = 180.0f + entityYaw * 20.0f
        entity.setYRot(180.0f + entityYaw * 40.0f)
        entity.setXRot(-entityPitch * 20.0f)
        entity.yHeadRot = entity.yRot
        entity.yHeadRotO = entity.yRot

        matrixStack.push()
        matrixStack.translate(0.0, 0.0, 1000.0)
        matrixStack.push()
        matrixStack.translate(x.toDouble(), y.toDouble(), 50.0)

        // UC's version of multiplyPositionMatrix
        matrixStack.peek().model.mul(
            Matrix4f().scaling(
                size.toFloat(),
                size.toFloat(),
                (-size).toFloat()
            )
        )

        matrixStack.multiply(flipModelRotation)
        // TODO: find out a way to get Diffuse instance and call setType
        // DiffuseLighting.enableGuiShaderLighting()

        val entityRenderDispatcher = Client.getMinecraft().entityRenderDispatcher

        if (pitchModelRotation != null) {
            pitchModelRotation.conjugate()
            entityRenderDispatcher.camera?.rotation()?.set(pitchModelRotation)
        }

        // entityRenderDispatcher.setRenderShadows(false)
//        val vertexConsumers = Client.getMinecraft().renderBuffers().bufferSource()

        // val light = 0xf000f0

        val entityRenderer = if (slim) slimCTRenderPlayer else normalCTRenderPlayer
        entityRenderer.setOptions(
            showNametag,
            showArmor,
            showCape,
            showHeldItem,
            showArrows,
            showElytra,
            showParrot,
            showStingers
        )

        val playerEntityRenderState = entityRenderer.createRenderState().apply {
            this.scale = size.toFloat()
            this.bodyRot = entity.yBodyRot
            this.yRot = entity.yRot
        }

        val vec3d = entityRenderer.getRenderOffset(playerEntityRenderState)
        val d = vec3d.x()
        val e = vec3d.y()
        val f = vec3d.z()
        matrixStack.push()
        matrixStack.translate(d, e, f)

        entityRenderer.submit(
            playerEntityRenderState,
            matrixStack.toMC(),
            (Client.getMinecraft().gameRenderer as GameRendererAccessor).submitNodeStorage,
            (Client.getMinecraft().levelRenderer as LevelRendererAccessor).levelRenderState.cameraRenderState
        )

        matrixStack.pop()

//        vertexConsumers.endBatch()
        // entityRenderDispatcher.setRenderShadows(true)
        matrixStack.pop()
        // TODO: find out a way to get Diffuse instance and call setType
        // DiffuseLighting.enableGuiDepthLighting()
        matrixStack.pop()

        entity.yBodyRot = oldBodyYaw
        entity.setYRot(oldYaw)
        entity.setXRot(oldPitch)
        entity.yHeadRotO = oldPrevHeadYaw
        entity.yHeadRot = oldHeadYaw

        matrixStack.pop()
    }

    internal fun withMatrix(stack: PoseStack?, partialTicks: Float = Renderer.partialTicks, block: () -> Unit) {
        Renderer.partialTicks = partialTicks
        matrixPushCounter = 0

        try {
            if (stack != null)
                pushMatrix(UMatrixStack(stack))

            block()
        } finally {
            if (stack != null)
                popMatrix()
        }

        if (matrixPushCounter > 0) {
            "Warning: Render function missing a call to Renderer.popMatrix()".printToConsole(LogType.WARN)
        } else if (matrixPushCounter < 0) {
            "Warning: Render function has too many calls to Renderer.popMatrix()".printToConsole(LogType.WARN)
        }
    }

    enum class DrawMode(private val ucValue: UGraphics.DrawMode) {
        LINES(UGraphics.DrawMode.LINES),
        LINE_STRIP(UGraphics.DrawMode.LINE_STRIP),
        TRIANGLES(UGraphics.DrawMode.TRIANGLES),
        TRIANGLE_STRIP(UGraphics.DrawMode.TRIANGLE_STRIP),
        TRIANGLE_FAN(UGraphics.DrawMode.TRIANGLE_FAN),
        QUADS(UGraphics.DrawMode.QUADS);

        fun toUC() = ucValue

        companion object {
            @JvmStatic
            fun fromUC(ucValue: UGraphics.DrawMode) = entries.first { it.ucValue == ucValue }
        }
    }

    enum class VertexFormat(private val mcValue: MCVertexFormat) {
        LINES(DefaultVertexFormat.POSITION_COLOR_NORMAL),
        POSITION(DefaultVertexFormat.POSITION),
        POSITION_COLOR(DefaultVertexFormat.POSITION_COLOR),
        POSITION_TEXTURE(DefaultVertexFormat.POSITION_TEX),
        POSITION_TEXTURE_COLOR(DefaultVertexFormat.POSITION_TEX_COLOR),
        POSITION_COLOR_TEXTURE_LIGHT(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
        POSITION_TEXTURE_LIGHT_COLOR(DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR),
        POSITION_TEXTURE_COLOR_LIGHT(DefaultVertexFormat.PARTICLE),
        POSITION_TEXTURE_COLOR_NORMAL(DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);

        fun toMC() = mcValue

        companion object {
            @JvmStatic
            fun fromMC(ucValue: MCVertexFormat) = entries.first { it.mcValue == ucValue }
        }
    }

    enum class RenderSnippet(val mcSnippet: Snippet) {
        TERRAIN_SNIPPET(RenderPipelines.TERRAIN_SNIPPET),
        ENTITY_SNIPPET(RenderPipelines.ENTITY_SNIPPET),
        RENDERTYPE_BEACON_BEAM_SNIPPET(RenderPipelines.BEACON_BEAM_SNIPPET),
        TEXT_SNIPPET(RenderPipelines.TEXT_SNIPPET),
        RENDERTYPE_END_PORTAL_SNIPPET(RenderPipelines.END_PORTAL_SNIPPET),
        RENDERTYPE_CLOUDS_SNIPPET(RenderPipelines.CLOUDS_SNIPPET),
        RENDERTYPE_LINES_SNIPPET(RenderPipelines.LINES_SNIPPET),
        POSITION_COLOR_SNIPPET(RenderPipelines.DEBUG_FILLED_SNIPPET),
        PARTICLE_SNIPPET(RenderPipelines.PARTICLE_SNIPPET),
        WEATHER_SNIPPET(RenderPipelines.WEATHER_SNIPPET),
        GUI_SNIPPET(RenderPipelines.GUI_SNIPPET),
        POSITION_TEX_COLOR_SNIPPET(RenderPipelines.GUI_TEXTURED_SNIPPET),
        RENDERTYPE_OUTLINE_SNIPPET(RenderPipelines.OUTLINE_SNIPPET),
        POST_EFFECT_PROCESSOR_SNIPPET(RenderPipelines.POST_PROCESSING_SNIPPET),
    }

    class ScreenWrapper {
        fun getWidth(): Int = Client.getMinecraft().window.guiScaledWidth

        fun getHeight(): Int = Client.getMinecraft().window.guiScaledHeight

        fun getScale(): Double = Client.getMinecraft().window.guiScale.toDouble()
    }
}
