package com.chattriggers.ctjs.api.render

import com.chattriggers.ctjs.internal.utils.getOption
import net.minecraft.client.gui.DrawContext
import org.mozilla.javascript.NativeObject
import java.util.concurrent.CopyOnWriteArrayList

class Display() {
    private var lines = CopyOnWriteArrayList<Text>()

    private var x = 0
    private var y = 0
    private var order = Order.NORMAL

    private var backgroundColor: Long = 0x50000000
    private var textColor: Long = 0xffffffff
    private var background = Background.NONE
    private var align = Text.Align.LEFT

    private var minWidth = 0
    private var width = 0
    private var height = 0

    constructor(config: NativeObject?) : this() {
        setBackgroundColor(config.getOption("backgroundColor", 0x50000000))
        setTextColor(config.getOption("textColor", 0xffffffff))
        setBackground(config.getOption("background", Background.NONE))
        setAlign(config.getOption("align", Text.Align.LEFT))
        setOrder(config.getOption("order", Order.NORMAL))
        setX(config.getOption("x", 0))
        setY(config.getOption("y", 0))
        setMinWidth(config.getOption("minWidth", 0))
    }

    fun getTextColor(): Long = textColor

    /**
     * Sets the color of the texts
     *
     * Overrides the color of the individual texts
     */
    fun setTextColor(textColor: Long) = apply {
        this.textColor = textColor
    }

    fun getAlign(): Text.Align = align

    /**
     * Set the alignment of the texts in the display
     *
     * Overrides alignment of the individual texts
     */
    fun setAlign(align: Any) = apply {
        this.align = when (align) {
            is CharSequence -> Text.Align.valueOf(align.toString().uppercase())
            is Text.Align -> align
            else -> Text.Align.LEFT
        }
    }

    fun getOrder(): Order = order

    fun setOrder(order: Any) = apply {
        this.order = when (order) {
            is CharSequence -> Order.valueOf(order.toString().uppercase())
            is Order -> order
            else -> Order.NORMAL
        }
    }

    fun getBackground(): Background = background

    fun setBackground(background: Any) = apply {
        this.background = when (background) {
            is CharSequence -> Background.valueOf(background.toString().uppercase().replace(" ", "_"))
            is Background -> background
            else -> Background.NONE
        }
    }

    fun getBackgroundColor(): Long = backgroundColor

    fun setBackgroundColor(backgroundColor: Long) = apply {
        this.backgroundColor = backgroundColor
    }

    fun setLine(index: Int, line: Any) = apply {
        while (lines.size - 1 < index)
            lines.add(Text(""))

        when (line) {
            is CharSequence -> lines[index].setString(line.toString())
            is Text -> lines[index] = line
            else -> lines[index] = Text("")
        }
    }

    fun getLine(index: Int): Text = lines[index]

    fun getLines(): List<Text> = lines

    fun setLines(lines: MutableList<Text>) = apply {
        this.lines = CopyOnWriteArrayList(lines)
    }

    fun addLine(line: Any) = apply {
        setLine(this.lines.size, line)
    }

    fun addLines(vararg lines: Any) = apply {
        lines.forEach { addLine(it) }
    }

    fun removeLine(index: Int) = apply {
        lines.removeAt(index)
    }

    fun clearLines() = apply {
        lines.clear()
    }

    fun getX(): Int = x

    fun setX(x: Int) = apply { this.x = x }

    fun getY(): Int = y

    fun setY(y: Int) = apply { this.y = y }

    fun getWidth(): Int = width

    fun getHeight(): Int = height

    fun getMinWidth(): Int = minWidth

    fun setMinWidth(minWidth: Int) = apply {
        this.minWidth = minWidth
    }

    fun draw(ctx: DrawContext) {
        width = lines.maxOfOrNull { it.getWidth() }?.coerceAtLeast(minWidth) ?: minWidth

        val textBackgroundWidth = when (background) {
            Background.FULL -> width
            Background.PER_LINE -> width
            Background.NONE -> null
        }

        var currentHeight = 0

        val linesX = when (align) {
            Text.Align.CENTER -> x + width / 2
            Text.Align.RIGHT -> x + width
            else -> x
        }

        val linesToDraw = when (order) {
            Order.NORMAL -> lines
            Order.REVERSED -> lines.asReversed()
        }

        linesToDraw.forEach {
            if (background === Background.FULL)
                it
                    .setBackground(true)
                    .setBackgroundColor(backgroundColor)

            it
                .setColor(textColor)
                .setAlign(align)
                .draw(ctx, linesX, y + currentHeight, x, textBackgroundWidth)

            currentHeight += it.getHeight().toInt()
        }

        height = currentHeight
    }

    override fun toString() =
        "Display{" +
            "renderX=$x, renderY=$y, " +
            "background=$background, backgroundColor=$backgroundColor, " +
            "textColor=$textColor, align=$align, order=$order, " +
            "minWidth=$minWidth, width=$width, height=$height, " +
            "lines=$lines" +
            "}"

    enum class Background {
        NONE, FULL, PER_LINE
    }

    enum class Order {
        REVERSED, NORMAL
    }
}
