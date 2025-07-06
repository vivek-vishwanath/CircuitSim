package com.ra4king.circuitsim.gui.peers.wiring

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.drawValue
import com.ra4king.circuitsim.gui.GuiUtils.drawValueOneLine
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @author Roi Atalla
 */
class Probe(props: Properties, x: Int, y: Int) : ComponentPeer<Component>(x, y, 0, 0) {
    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.DIRECTION)
        properties.ensureProperty(Properties.BITSIZE)
        properties.ensureProperty(Properties.BASE)
        properties.mergeIfExists(props)

        val bitSize = properties.getValue(Properties.BITSIZE)

        val probe: Component = object : Component(properties.getValue(Properties.LABEL), intArrayOf(bitSize)) {
            override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {}
        }

        when (properties.getValue(Properties.BASE)) {
            Properties.Base.BINARY -> {
                width = max(2, min(8, bitSize))
                height = ((1 + (bitSize - 1) / 8) * 1.5).roundToInt()
            }

            Properties.Base.HEXADECIMAL -> {
                width = max(2, 1 + (bitSize - 1) / 4)
                height = 2
            }

            Properties.Base.DECIMAL -> {
                // 3.322 ~ log_2(10)
                var width = max(2, ceil(bitSize / 3.322).toInt())
                width += if (bitSize == 32) 1 else 0
                this.width = width
                height = 2
            }
        }

        val connections = ArrayList<PortConnection>()
        when (properties.getValue(Properties.DIRECTION)) {
            Properties.Direction.EAST -> connections.add(PortConnection(this, probe.getPort(0), width, height / 2))
            Properties.Direction.WEST -> connections.add(PortConnection(this, probe.getPort(0), 0, height / 2))
            Properties.Direction.NORTH -> connections.add(PortConnection(this, probe.getPort(0), width / 2, 0))
            Properties.Direction.SOUTH -> connections.add(PortConnection(this, probe.getPort(0), width / 2, height))
        }

        init(probe, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))

        graphics.font = getFont(16)
        val port = component.getPort(0)

        val value = circuitState!!.getLastReceived(port)
        val valStr = when (properties.getValue(Properties.BASE)) {
            Properties.Base.BINARY -> value.toString()
            Properties.Base.HEXADECIMAL -> value.hexString
            Properties.Base.DECIMAL -> value.decString
        }

        if (circuitState.isShortCircuited(port.link)) {
            graphics.fill = Color.RED
        } else {
            graphics.fill = Color.LIGHTGRAY
        }

        graphics.stroke = Color.WHITE

        graphics.fillRoundRect(
            screenX.toDouble(),
            screenY.toDouble(),
            screenWidth.toDouble(),
            screenHeight.toDouble(),
            20.0,
            20.0
        )
        graphics.strokeRoundRect(
            screenX.toDouble(),
            screenY.toDouble(),
            screenWidth.toDouble(),
            screenHeight.toDouble(),
            20.0,
            20.0
        )

        graphics.fill = Color.BLACK
        if (properties.getValue(Properties.BASE) == Properties.Base.DECIMAL) {
            drawValueOneLine(graphics, valStr, screenX, screenY, screenWidth)
        } else {
            drawValue(graphics, valStr, screenX, screenY, screenWidth)
        }
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Wiring", "Probe"),
                Image(Probe::class.java.getResourceAsStream("/images/Probe.png")),
                Properties(
                    Properties.Property(
                        Properties.DIRECTION,
                        Properties.Direction.SOUTH
                    )
                ), true
            )
        }
    }
}
