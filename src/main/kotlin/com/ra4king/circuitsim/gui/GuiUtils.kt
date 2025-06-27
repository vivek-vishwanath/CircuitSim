package com.ra4king.circuitsim.gui

import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.WireValue
import javafx.geometry.Bounds
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @author Roi Atalla
 */
object GuiUtils {
    const val BLOCK_SIZE: Int = 10

    private val fonts: MutableMap<FontInfo, Font> = HashMap<FontInfo, Font>()

    @JvmOverloads
    @JvmStatic
    fun getFont(size: Int, bold: Boolean = false, oblique: Boolean = false): Font {
        val info = FontInfo(size, bold, oblique)
        return fonts[info] ?: run {
            val fontFile = when {
                bold && oblique -> "/fonts/DejaVuSansMono-BoldOblique.ttf"
                bold -> "/fonts/DejaVuSansMono-Bold.ttf"
                oblique -> "/fonts/DejaVuSansMono-Oblique.ttf"
                else -> "/fonts/DejaVuSansMono.ttf"
            }
            val font = Font.loadFont(GuiUtils::class.java.getResourceAsStream(fontFile), size.toDouble())
            fonts.put(info, font)
            font
        }
    }

    /**
     * Converts a coordinate value in the canvas coordinate system to the circuit coordinate system.
     *
     * See CircuitManager for details about the coordinate systems.
     *
     * @param a a canvas coordinate value
     * @return a circuit coordinate value
     *
     * @see CircuitManager CircuitManager
     */
    @JvmStatic
    fun getCircuitCoord(a: Double): Int = (a.roundToInt() + BLOCK_SIZE / 2) / BLOCK_SIZE

    /**
     * Converts a coordinate value in the canvas coordinate system to the canvas coordinate
     * that corresponds to the center of the circuit tile.
     *
     * See CircuitManager for details about the coordinate systems.
     *
     * @param a a canvas coordinate value
     * @return a new canvas coordinate value
     *
     * @see CircuitManager CircuitManager
     */
    @JvmStatic
    fun getScreenCircuitCoord(a: Double) = getCircuitCoord(a) * BLOCK_SIZE

    private val boundsSeen = HashMap<Font, MutableMap<String, Bounds>>()

    @JvmOverloads
    @JvmStatic
    fun getBounds(font: Font, string: String, save: Boolean = true): Bounds = if (save) {
        val strings = boundsSeen.computeIfAbsent(font) { f -> HashMap<String, Bounds>() }
        strings.computeIfAbsent(string) { s: String ->
            val text = Text(string)
            text.font = font
            text.layoutBounds
        }
    } else {
        val text = Text(string)
        text.font = font
        text.layoutBounds
    }

    fun drawShape(element: GuiElement, drawable: (Double, Double, Double, Double) -> Unit) {
        drawable(element.screenX.toDouble(), element.screenY.toDouble(),
            element.screenWidth.toDouble(), element.screenHeight.toDouble()
        )
    }

    @JvmStatic
    fun drawShape(drawable: Drawable, element: GuiElement) = drawShape(element) { v1, v2, v3, v4 -> drawable.draw(v1, v2, v3, v4)}

    @JvmStatic
    fun drawName(graphics: GraphicsContext, component: ComponentPeer<*>, direction: Properties.Direction) {
        if (!component.component.name.isEmpty()) {
            val bounds = getBounds(graphics.font, component.component.name)
            val coord = when (direction) {
                Properties.Direction.EAST -> Pair(
                    component.screenX + component.screenWidth + 5.0,
                    component.screenY + (component.screenHeight + bounds.height) * 0.4
                )

                Properties.Direction.WEST -> Pair(
                    component.screenX - bounds.width - 3,
                    component.screenY + (component.screenHeight + bounds.height) * 0.4
                )

                Properties.Direction.SOUTH -> Pair(
                    component.screenX + (component.screenWidth - bounds.width) * 0.5,
                    component.screenY + component.screenHeight + bounds.height
                )

                Properties.Direction.NORTH -> Pair(
                    component.screenX + (component.screenWidth - bounds.width) * 0.5,
                    component.screenY - 5.0
                )
            }
            graphics.fill = Color.BLACK
            graphics.fillText(component.component.name, coord.first, coord.second)
        }
    }

    @JvmStatic
    fun drawValue(graphics: GraphicsContext, string: String, x: Int, y: Int, width: Int) {
        val bounds = getBounds(graphics.font, string, false)

        if (string.length == 1) {
            graphics.fillText(string, x + (width - bounds.width) * 0.5, y + bounds.height * 0.75 + 1)
        } else {
            var i = 0
            var row = 1
            while (i < string.length) {
                val sub = string.substring(i, i + min(8, string.length - i))
                i += sub.length
                graphics.fillText(sub, x + 1.0, y + bounds.height * 0.75 * row + 1)
                row++
            }
        }
    }

    @JvmStatic
    fun drawValueOneLine(graphics: GraphicsContext, string: String, x: Int, y: Int, width: Int) {
        val bounds = getBounds(graphics.font, string, false)

        graphics.fillText(
            string,
            if (string.length == 1) (x + (width - bounds.width) * 0.5) else x + 1.0,
            y + bounds.height * 0.75 + 1
        )
    }

    /**
     * Draws a clock input (triangle symbol) facing the southern border
     */
    @JvmStatic
    fun drawClockInput(graphics: GraphicsContext, connection: Connection, direction: Properties.Direction) {
        val x = connection.screenX + connection.screenWidth * 0.5
        val y = connection.screenY + connection.screenWidth * 0.5

        when (direction) {
            Properties.Direction.NORTH -> {
                graphics.strokeLine(x - 5, y, x, y + 6)
                graphics.strokeLine(x, y + 6, x + 5, y)
            }

            Properties.Direction.SOUTH -> {
                graphics.strokeLine(x - 5, y, x, y - 6)
                graphics.strokeLine(x, y - 6, x + 5, y)
            }

            Properties.Direction.EAST -> {
                graphics.strokeLine(x, y - 5, x - 6, y)
                graphics.strokeLine(x - 6, y, x, y + 5)
            }

            Properties.Direction.WEST -> {
                graphics.strokeLine(x, y - 5, x + 6, y)
                graphics.strokeLine(x + 6, y, x, y + 5)
            }
        }
    }

    @JvmStatic
    fun setBitColor(graphics: GraphicsContext, circuitState: CircuitState?, linkWires: LinkWires) {
        if (linkWires.isLinkValid) {
            val link = linkWires.link
            if (link != null && circuitState != null) {
                if (circuitState.isShortCircuited(link)) {
                    graphics.stroke = Color.RED
                    graphics.fill = Color.RED
                } else {
                    setBitColor(graphics, circuitState.getMergedValue(link))
                }
            } else {
                setBitColor(graphics, WireValue.State.Z)
            }
        } else {
            graphics.stroke = Color.ORANGE
            graphics.fill = Color.ORANGE
        }
    }

    private val ONE_COLOR: Color = Color.GREEN.brighter()
    private val ZERO_COLOR: Color = Color.GREEN.darker()
    private val Z_1BIT_COLOR: Color = Color.BLUE
    private val Z_MULTIBIT_COLOR: Color = Color.BLUE.darker()

    @JvmStatic
    fun setBitColor(graphics: GraphicsContext, value: WireValue) {
        if (value.bitSize == 1) {
            setBitColor(graphics, value.getBit(0))
        } else if (value.isValidValue) {
            graphics.stroke = Color.BLACK
            graphics.fill = Color.BLACK
        } else {
            graphics.stroke = Z_MULTIBIT_COLOR
            graphics.fill = Z_MULTIBIT_COLOR
        }
    }

    @JvmStatic
    fun setBitColor(graphics: GraphicsContext, bitState: WireValue.State) {
        when (bitState) {
            WireValue.State.ONE -> {
                graphics.stroke = ONE_COLOR
                graphics.fill = ONE_COLOR
            }

            WireValue.State.ZERO -> {
                graphics.stroke = ZERO_COLOR
                graphics.fill = ZERO_COLOR
            }

            WireValue.State.Z -> {
                graphics.stroke = Z_1BIT_COLOR
                graphics.fill = Z_1BIT_COLOR
            }
        }
    }

    private fun rotatePortCCW(connection: PortConnection, useWidth: Boolean): PortConnection {
        val x = connection.xOffset
        val y = connection.yOffset
        val width = if (useWidth) connection.parent.width else connection.parent.height

        return PortConnection(connection.parent, connection.port, connection.name, y, width - x)
    }

    @JvmStatic
    fun rotatePorts(
        connections: MutableList<PortConnection>,
        source: Properties.Direction,
        destination: Properties.Direction
    ) {
        val order = listOf(
            Properties.Direction.EAST,
            Properties.Direction.NORTH,
            Properties.Direction.WEST,
            Properties.Direction.SOUTH
        )

        var stream: Stream<PortConnection> = connections.stream()

        var index = order.indexOf(source)
        var useWidth = true
        while (order[index++ % order.size] != destination) {
            val temp = useWidth
            stream = stream.map { port: PortConnection -> rotatePortCCW(port, temp) }
            useWidth = !useWidth
        }

        val newConns = stream.collect(Collectors.toList())
        connections.clear()
        connections.addAll(newConns)
    }

    @JvmStatic
    fun rotateElementSize(element: GuiElement, source: Properties.Direction, destination: Properties.Direction) {
        val order = listOf(
            Properties.Direction.EAST,
            Properties.Direction.NORTH,
            Properties.Direction.WEST,
            Properties.Direction.SOUTH
        )

        var index = order.indexOf(source)
        while (order[index++ % order.size] != destination) {
            val width = element.width
            val height = element.height
            element.width = height
            element.height = width
        }
    }

    /**
     * Source orientation is assumed EAST
     */
    @JvmStatic
    fun rotateGraphics(element: GuiElement, graphics: GraphicsContext, direction: Properties.Direction) {
        val x = element.screenX
        val y = element.screenY
        val width = element.screenWidth
        val height = element.screenHeight

        graphics.translate(x + width * 0.5, y + height * 0.5)
        when (direction) {
            Properties.Direction.NORTH -> {
                graphics.rotate(270.0)
                graphics.translate(-x - height * 0.5, -y - width * 0.5)
            }

            Properties.Direction.SOUTH -> {
                graphics.rotate(90.0)
                graphics.translate(-x - height * 0.5, -y - width * 0.5)
            }

            Properties.Direction.WEST -> {
                graphics.rotate(180.0)
                graphics.translate(-x - width * 0.5, -y - height * 0.5)
            }

            Properties.Direction.EAST -> graphics.translate(-x - width * 0.5, -y - height * 0.5)
        }
    }

    private class FontInfo(val size: Int, val bold: Boolean, val oblique: Boolean) {
        override fun hashCode(): Int = size xor (if (bold) 0x1000 else 0) xor (if (oblique) 0x2000 else 0)
        override fun equals(other: Any?) = other is FontInfo && other.size == this.size && other.bold == this.bold && other.oblique == this.oblique
    }

    fun interface Drawable {
        fun draw(x: Double, y: Double, width: Double, height: Double)
    }
}
