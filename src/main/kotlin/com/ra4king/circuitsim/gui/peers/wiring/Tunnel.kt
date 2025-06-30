package com.ra4king.circuitsim.gui.peers.wiring

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils
import com.ra4king.circuitsim.gui.GuiUtils.getBounds
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.properties.IntegerString
import com.ra4king.circuitsim.gui.properties.PropertyValidators
import com.ra4king.circuitsim.simulator.Circuit
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import kotlin.math.ceil
import kotlin.math.max

/**
 * @author Roi Atalla
 */
class Tunnel(props: Properties, x: Int, y: Int) : ComponentPeer<Component>(x, y, 0, 2) {
    private val tunnel: Component
    private val label: String
    private val bitSize: Int

    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.DIRECTION)
        properties.ensureProperty(Properties.BITSIZE)
        properties.ensureProperty(WIDTH)
        properties.mergeIfExists(props)

        label = properties.getValue(Properties.LABEL)
        bitSize = properties.getValue(Properties.BITSIZE)

        // Avoid recalculating the width if the previous text hasn't changed
        this.width =
            if (props.containsProperty(WIDTH) && (!props.containsProperty(PREVIOUS_TEXT) || props.getValue(PREVIOUS_TEXT) == label))
                properties.getValue(WIDTH).value
            else {
                val bounds = getBounds(getFont(13), label)
                val width = max(ceil(bounds.width / GuiUtils.BLOCK_SIZE).toInt(), 1)
                properties.setValue(WIDTH, IntegerString(width))
                width
            }
        properties.setValue(PREVIOUS_TEXT, label)

        tunnel = object : Component(label, intArrayOf(bitSize)) {
            override var circuit: Circuit? = null
                set(value) {
                    val oldCircuit = circuit
                    field = value
                    if (label.isEmpty()) return

                    if (value != null) {
                        val tunnelSet = tunnels.computeIfAbsent(value) { HashMap() }
                        val toNotify = tunnelSet.computeIfAbsent(label) { HashSet() }
                        toNotify.add(this@Tunnel)
                    } else {
                        val tunnelSet = tunnels[oldCircuit] ?: return
                        val toNotify = tunnelSet[label] ?: return

                        toNotify.remove(this@Tunnel)
                        if (toNotify.isEmpty()) {
                            tunnelSet.remove(label)
                            if (tunnelSet.isEmpty()) {
                                tunnels.remove(oldCircuit)
                            }
                        }
                    }
                }

            override fun init(circuitState: CircuitState, lastProperty: Any?) {
                if (label.isEmpty()) return

                val tunnelSet = tunnels[circuit] ?: return
                val toNotify: MutableSet<Tunnel> = tunnelSet[label]!!
                val value = WireValue(bitSize)

                for (tunnel in toNotify) {
                    if (tunnel !== this@Tunnel) {
                        val port = tunnel.component.getPort(0)
                        val portValue = circuitState.getLastReceived(port)
                        if (portValue.bitSize == value.bitSize) {
                            try {
                                value.merge(portValue)
                            } catch (_: Exception) {
                                return  // nothing to push, it's a short circuit
                            }
                        }
                    }

                    circuitState.pushValue(getPort(0), value)
                }
            }

            override fun uninit(circuitState: CircuitState) {
                val tunnelSet = tunnels[circuit] ?: return
                val toNotify = tunnelSet[label]
                if (toNotify != null) {
                    tunnels@ for (tunnel in toNotify) {
                        if (tunnel.bitSize == bitSize) {
                            val combined = WireValue(bitSize)

                            for (otherTunnel in toNotify) {
                                if (tunnel !== otherTunnel && otherTunnel !== this@Tunnel) {
                                    val port = otherTunnel.component.getPort(0)
                                    val portValue = circuitState.getLastReceived(port)
                                    if (portValue.bitSize == combined.bitSize) {
                                        try {
                                            combined.merge(portValue)
                                        } catch (_: Exception) {
                                            continue@tunnels
                                        }
                                    }
                                }
                            }

                            circuitState.pushValue(tunnel.component.getPort(0), combined)
                        }
                    }
                }
            }

            override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
                val tunnelSet = tunnels[circuit] ?: return
                val toNotify = tunnelSet[label] ?: return

                tunnels@ for (tunnel in toNotify) {
                    if (tunnel !== this@Tunnel && tunnel.bitSize == bitSize) {
                        var combined = value

                        if (toNotify.size > 2) {
                            combined = WireValue(bitSize)

                            for (otherTunnel in toNotify) {
                                if (tunnel !== otherTunnel) {
                                    val port = otherTunnel.component.getPort(0)
                                    val portValue = state.getLastReceived(port)
                                    if (portValue.bitSize == combined.bitSize) {
                                        try {
                                            combined.merge(portValue)
                                        } catch (_: Exception) {
                                            continue@tunnels
                                        }
                                    }
                                }
                            }
                        }

                        state.pushValue(tunnel.component.getPort(0), combined)
                    }
                }
            }
        }

        val connections = ArrayList<PortConnection>()
        when (properties.getValue(Properties.DIRECTION)) {
            Properties.Direction.EAST -> {
                this.width = width + 2
                connections.add(PortConnection(this, tunnel.getPort(0), this.width, height / 2))
            }

            Properties.Direction.WEST -> {
                this.width = width + 2
                connections.add(PortConnection(this, tunnel.getPort(0), 0, height / 2))
            }

            Properties.Direction.NORTH -> {
                this.width = max(((width - 1) / 2) * 2 + 2, 2)
                height = 3
                connections.add(PortConnection(this, tunnel.getPort(0), this.width / 2, 0))
            }

            Properties.Direction.SOUTH -> {
                this.width = max(((width - 1) / 2) * 2 + 2, 2)
                height = 3
                connections.add(PortConnection(this, tunnel.getPort(0), this.width / 2, height))
            }
        }

        init(tunnel, properties, connections)
    }

    private val isIncompatible: Boolean
        get() {
            val tunnelMap = tunnels[tunnel.circuit] ?: return false
            val tunnelSet = tunnelMap[label] ?: return false
            for (tunnel in tunnelSet) {
                if (tunnel.bitSize != bitSize) return true
            }

            return false
        }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        val direction = properties.getValue(Properties.DIRECTION)

        val isIncompatible = this.isIncompatible

        graphics.stroke = Color.BLACK
        graphics.fill = if (isIncompatible) Color.ORANGE else Color.WHITE

        val block = GuiUtils.BLOCK_SIZE
        val x = screenX
        val y = screenY
        val width = screenWidth
        val height = screenHeight

        var xOff = 0
        var yOff = 0

        when (direction) {
            Properties.Direction.EAST -> {
                xOff = -block
                graphics.beginPath()
                graphics.moveTo((x + width).toDouble(), y + height * 0.5)
                graphics.lineTo((x + width - block).toDouble(), (y + height).toDouble())
                graphics.lineTo(x.toDouble(), (y + height).toDouble())
                graphics.lineTo(x.toDouble(), y.toDouble())
                graphics.lineTo((x + width - block).toDouble(), y.toDouble())
                graphics.closePath()
            }

            Properties.Direction.WEST -> {
                xOff = block
                graphics.beginPath()
                graphics.moveTo(x.toDouble(), y + height * 0.5)
                graphics.lineTo((x + block).toDouble(), y.toDouble())
                graphics.lineTo((x + width).toDouble(), y.toDouble())
                graphics.lineTo((x + width).toDouble(), (y + height).toDouble())
                graphics.lineTo((x + block).toDouble(), (y + height).toDouble())
                graphics.closePath()
            }

            Properties.Direction.NORTH -> {
                yOff = block
                graphics.beginPath()
                graphics.moveTo(x + width * 0.5, y.toDouble())
                graphics.lineTo((x + width).toDouble(), (y + block).toDouble())
                graphics.lineTo((x + width).toDouble(), (y + height).toDouble())
                graphics.lineTo(x.toDouble(), (y + height).toDouble())
                graphics.lineTo(x.toDouble(), (y + block).toDouble())
                graphics.closePath()
            }

            Properties.Direction.SOUTH -> {
                yOff = -block
                graphics.beginPath()
                graphics.moveTo(x + width * 0.5, (y + height).toDouble())
                graphics.lineTo(x.toDouble(), (y + height - block).toDouble())
                graphics.lineTo(x.toDouble(), y.toDouble())
                graphics.lineTo((x + width).toDouble(), y.toDouble())
                graphics.lineTo((x + width).toDouble(), (y + height - block).toDouble())
                graphics.closePath()
            }
        }

        graphics.fill()
        graphics.stroke()

        if (!label.isEmpty()) {
            val bounds = getBounds(graphics.font, label)
            graphics.fill = Color.BLACK
            graphics.fillText(
                label,
                x + xOff + ((width - xOff) - bounds.width) * 0.5,
                y + yOff + ((height - yOff) + bounds.height) * 0.4
            )
        }

        if (isIncompatible) {
            val port = connections[0]

            graphics.fill = Color.BLACK
            graphics.fillText(bitSize.toString(), (port.screenX + 11).toDouble(), (port.screenY + 21).toDouble())

            graphics.stroke = Color.ORANGE
            graphics.fill = Color.ORANGE
            graphics.strokeOval((port.screenX - 2).toDouble(), (port.screenY - 2).toDouble(), 10.0, 10.0)
            graphics.fillText(bitSize.toString(), (port.screenX + 10).toDouble(), (port.screenY + 20).toDouble())
        }
    }

    companion object {
        private val tunnels = HashMap<Circuit?, MutableMap<String?, MutableSet<Tunnel>>>()

        private val WIDTH = Properties.Property(
            "Width",
            "",
            "",
            PropertyValidators.INTEGER_VALIDATOR,
            true,
            false,
            IntegerString(0)
        )
        private val PREVIOUS_TEXT = Properties.Property(
            "Previous text",
            "",
            "",
            PropertyValidators.ANY_STRING_VALIDATOR,
            true,
            true,
            ""
        )

        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Wiring", "Tunnel"),
                Image(Tunnel::class.java.getResourceAsStream("/images/Tunnel.png")),
                Properties(Properties.Property(Properties.DIRECTION, Properties.Direction.WEST)),
                true
            )
        }
    }
}
