package com.ra4king.circuitsim.gui.peers

import com.ra4king.circuitsim.gui.CircuitManager
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.peers.wiring.PinPeer
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.SimulationException
import com.ra4king.circuitsim.simulator.components.Subcircuit
import javafx.event.EventHandler
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.MenuItem
import kotlin.math.max

/**
 * @author Roi Atalla
 */
class SubcircuitPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Subcircuit>(x, y, 0, 0) {
    fun switchToSubcircuit(circuit: CircuitManager) {
        circuit.simulatorWindow.switchToCircuit(
            component.subcircuit,
            component.getSubcircuitState(circuit.circuitBoard.currentState)
        )
    }

    override fun getContextMenuItems(circuit: CircuitManager): MutableList<MenuItem> {
        val view = MenuItem("View internal state")
        view.onAction = EventHandler { switchToSubcircuit(circuit) }
        return mutableListOf(view)
    }

    private var mouseEntered = false

    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.mergeIfExists(props)

        val subcircuitProperty = props.getProperty<CircuitManager?>(SUBCIRCUIT)
        properties.setProperty(subcircuitProperty)

        val subcircuitManager = subcircuitProperty.value
        if (subcircuitManager == null) {
            throw SimulationException("Circuit does not exist")
        }
        val subcircuit = Subcircuit(properties.getValue(Properties.LABEL), subcircuitManager.circuit)

        val connections = ArrayList<PortConnection>()
        val pins = subcircuitManager.circuitBoard.components.filterIsInstance<PinPeer>()
        val eastPins =
            pins.filter { pin: PinPeer -> pin.properties.getValue(Properties.DIRECTION) == Properties.Direction.EAST }
                .sortedWith { o1: PinPeer, o2: PinPeer ->
                    val diff = o1.y - o2.y
                    return@sortedWith if (diff == 0) o1.x - o2.x else diff
                }
        val westPins =
            pins.filter { pin: PinPeer -> pin.properties.getValue(Properties.DIRECTION) == Properties.Direction.WEST }
                .sortedWith { o1: PinPeer, o2: PinPeer ->
                    val diff = o1.y - o2.y
                    return@sortedWith if (diff == 0) o1.x - o2.x else diff
                }
        val northPins =
            pins.filter { pin: PinPeer -> pin.properties.getValue(Properties.DIRECTION) == Properties.Direction.NORTH }
                .sortedWith { o1: PinPeer, o2: PinPeer ->
                    val diff = o1.x - o2.x
                    return@sortedWith if (diff == 0) o1.y - o2.y else diff
                }
        val southPins =
            pins.filter { pin: PinPeer -> pin.properties.getValue(Properties.DIRECTION) == Properties.Direction.SOUTH }
                .sortedWith { o1: PinPeer, o2: PinPeer ->
                    val diff = o1.x - o2.x
                    return@sortedWith if (diff == 0) o1.y - o2.y else diff
                }

        check(pins.size == subcircuit.numPorts) { "Pin count and ports count don't match? " + pins.size + " vs " + subcircuit.numPorts }

        width = max(3, max(northPins.size, southPins.size) + 1)
        height = max(3, max(eastPins.size, westPins.size) + 1)

        for (i in eastPins.indices) {
            val connX = 0
            val connY = i + 1
            connections.add(
                PortConnection(
                    this,
                    subcircuit.getPort(eastPins[i].component)!!,
                    eastPins[i].component.name,
                    connX,
                    connY
                )
            )
        }

        for (i in westPins.indices) {
            val connX = width
            val connY = i + 1
            connections.add(
                PortConnection(
                    this, subcircuit.getPort(westPins[i].component)!!,
                    westPins[i].component.name, connX, connY
                )
            )
        }

        for (i in northPins.indices) {
            val connX = i + 1
            val connY = height
            connections.add(
                PortConnection(
                    this, subcircuit.getPort(northPins[i].component)!!,
                    northPins[i].component.name, connX, connY
                )
            )
        }

        for (i in southPins.indices) {
            val connX = i + 1
            val connY = 0
            connections.add(
                PortConnection(
                    this, subcircuit.getPort(southPins[i].component)!!,
                    southPins[i].component.name, connX, connY
                )
            )
        }

        init(subcircuit, properties, connections)
    }

    override fun mouseEntered(manager: CircuitManager, state: CircuitState) {
        mouseEntered = true
    }

    override fun mouseExited(manager: CircuitManager, state: CircuitState) {
        mouseEntered = false
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        if (mouseEntered) {
            val width = screenWidth.toDouble()
            val height = screenHeight.toDouble()

            graphics.lineWidth = 1.5
            graphics.strokeOval(screenX + (width - 13) / 2, screenY + (height - 13) / 2, 13.0, 13.0)

            graphics.lineWidth = 2.5
            graphics.strokeLine(
                screenX + width / 2 + 4.6,
                screenY + height / 2 + 4.6,
                screenX + width / 2 + 10,
                screenY + height / 2 + 10
            )
        }
    }

    companion object {
        const val SUBCIRCUIT: String = "Subcircuit"
    }
}
