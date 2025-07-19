package com.ra4king.circuitsim.gui.peers.wiring

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.rotateElementSize
import com.ra4king.circuitsim.gui.GuiUtils.rotateGraphics
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.properties.PropertyListValidator
import com.ra4king.circuitsim.gui.properties.PropertyValidators
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.wiring.Splitter
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import kotlin.math.max
import kotlin.math.min

/**
 * @author Roi Atalla
 */
class SplitterPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Splitter>(x, y, 2, 0) {
    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.DIRECTION)
        properties.ensureProperty(INPUT_LOCATION)
        properties.ensureProperty(FANOUTS)
        properties.ensureProperty(Properties.BITSIZE)
        properties.mergeIfExists(props)

        val bitSize = properties.getValue(Properties.BITSIZE)
        val numInputs: Int = properties.getValue(FANOUTS)

        val fanOuts = ArrayList<Int>()
        for (i in -1..<numInputs) {
            fanOuts.add(i)
        }

        val validator = PropertyListValidator(
            fanOuts
        ) { value -> if (value == -1) "None" else value.toString() }

        val splitter: Splitter?

        var availableBits = 0
        while (props.containsProperty("Bit $availableBits")) {
            availableBits++
        }

        if (availableBits == bitSize) {
            val bitFanIndices = IntArray(bitSize)

            for (i in bitFanIndices.indices) {
                val value = props.getValue<Any?>("Bit $i")
                val index: Int = when (value) {
                    null -> i
                    is String -> validator.parse(value)!!
                    else -> value as Int
                }
                bitFanIndices[i] = min(numInputs - 1, index)
            }

            splitter = Splitter(properties.getValue(Properties.LABEL), bitFanIndices)
        } else {
            splitter = Splitter(properties.getValue(Properties.LABEL), bitSize, numInputs)
        }

        height = max(2, splitter.numPorts)

        val bitFanIndices = splitter.bitFanIndices
        for (i in bitFanIndices.indices) {
            properties.setProperty(Properties.Property("Bit $i", validator, bitFanIndices[i]))
        }

        val direction = properties.getValue(Properties.DIRECTION)
        val inputOnTopLeft: Boolean = properties.getValue(INPUT_LOCATION)

        rotateElementSize(this, Properties.Direction.EAST, direction)

        val connections = ArrayList<PortConnection>()
        for (i in 0..<splitter.numPorts - 1) {
            val tooltip = StringBuilder()
            var j = 0
            var start = -1
            while (j < bitFanIndices.size) {
                if (bitFanIndices[j] == splitter.numPorts - 2 - i) {
                    if (start == -1 || j == bitFanIndices.size - 1) {
                        tooltip.append(if (start == -1) ',' else '-').append(j)
                        start = j
                    }
                } else if (start != -1) {
                    if (start < j - 1) {
                        tooltip.append('-').append(j - 1)
                    }

                    start = -1
                }
                j++
            }

            val cx: Int
            val cy: Int
            when (direction) {
                Properties.Direction.EAST -> {
                    cx = width
                    cy = if (inputOnTopLeft) i + 2 else height - i - 2
                }

                Properties.Direction.WEST -> {
                    cx = 0
                    cy = if (inputOnTopLeft) i + 2 else height - i - 2
                }

                Properties.Direction.SOUTH -> {
                    cx = if (inputOnTopLeft) i + 2 else width - i - 2
                    cy = height
                }

                Properties.Direction.NORTH -> {
                    cx = if (inputOnTopLeft) i + 2 else width - i - 2
                    cy = 0
                }

                else -> throw IllegalArgumentException("Why are you doing this?")
            }

            connections.add(
                PortConnection(this,splitter.getPort(splitter.numPorts - 2 - i),if (tooltip.isEmpty()) tooltip.toString() else tooltip.substring(1),cx,cy
                )
            )
        }

        when (direction) {
            Properties.Direction.EAST -> connections.add(PortConnection(this,splitter.getPort(splitter.portJoined),0,if (inputOnTopLeft) 0 else height))

            Properties.Direction.WEST -> connections.add(PortConnection(this,splitter.getPort(splitter.portJoined),width,if (inputOnTopLeft) 0 else height))

            Properties.Direction.SOUTH -> connections.add(PortConnection(this,splitter.getPort(splitter.portJoined),if (inputOnTopLeft) 0 else width,0))

            Properties.Direction.NORTH -> connections.add(PortConnection(this,splitter.getPort(splitter.portJoined),if (inputOnTopLeft) 0 else width,height))
        }

        init(splitter, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))

        val direction = properties.getValue(Properties.DIRECTION)
        var inputOnTop: Boolean = properties.getValue(INPUT_LOCATION)
        when (direction) {
            Properties.Direction.SOUTH, Properties.Direction.WEST -> {
                inputOnTop = !inputOnTop
                rotateGraphics(this, graphics, direction)

                val x = screenX.toDouble()
                val y = screenY.toDouble()
                val height = max(screenWidth, screenHeight)

                graphics.lineWidth = 3.0
                graphics.stroke = Color.BLACK

                val y2 = y + (if (inputOnTop) GuiUtils.BLOCK_SIZE else height - GuiUtils.BLOCK_SIZE)
                graphics.strokeLine(x,y + (if (inputOnTop) 0 else height),x + GuiUtils.BLOCK_SIZE,y2)
                graphics.strokeLine(x + GuiUtils.BLOCK_SIZE,y2,x + GuiUtils.BLOCK_SIZE,y + (if (inputOnTop) height else 0))

                var i = 2 * GuiUtils.BLOCK_SIZE
                while (i <= height) {
                    val offset = if (inputOnTop) 0 else 2 * GuiUtils.BLOCK_SIZE
                    graphics.strokeLine(    x + GuiUtils.BLOCK_SIZE,    y + i - offset,    x + 2 * GuiUtils.BLOCK_SIZE,    y + i - offset)
                    i += GuiUtils.BLOCK_SIZE
                }
            }

            Properties.Direction.EAST, Properties.Direction.NORTH -> {
                rotateGraphics(this, graphics, direction)

                val x = screenX.toDouble()
                val y = screenY.toDouble()
                val height = max(screenWidth, screenHeight)

                graphics.lineWidth = 3.0
                graphics.stroke = Color.BLACK

                val y2 = y + if (inputOnTop) GuiUtils.BLOCK_SIZE else height - GuiUtils.BLOCK_SIZE
                graphics.strokeLine(x,y + (if (inputOnTop) 0 else height),x + GuiUtils.BLOCK_SIZE,y2)
                graphics.strokeLine(x + GuiUtils.BLOCK_SIZE,y2,x + GuiUtils.BLOCK_SIZE,y + (if (inputOnTop) height else 0))

                var i = 2 * GuiUtils.BLOCK_SIZE
                while (i <= height) {
                    val offset = if (inputOnTop) 0 else 2 * GuiUtils.BLOCK_SIZE
                    graphics.strokeLine(    x + GuiUtils.BLOCK_SIZE,    y + i - offset,    x + 2 * GuiUtils.BLOCK_SIZE,    y + i - offset)
                    i += GuiUtils.BLOCK_SIZE
                }
            }
        }
    }

    companion object {
        private val FANOUTS =
            Properties.Property("Fanouts", PropertyListValidator(Array(32) { it + 1 }.toMutableList()), 2)

        private val INPUT_LOCATION =
            Properties.Property("Input location", PropertyValidators.LOCATION_VALIDATOR, true)

        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Wiring", "Splitter"),
                Image(SplitterPeer::class.java.getResourceAsStream("/images/Splitter.png")),
                Properties(Properties.Property(Properties.BITSIZE, 2)), true
            )
        }
    }
}
