package com.ra4king.circuitsim.gui.peers.io

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class SevenSegmentDisplay(props: Properties, x: Int, y: Int) : ComponentPeer<Component>(x, y, 4, 6) {
    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.mergeIfExists(props)

        val component: Component = object : Component(properties.getValue(Properties.LABEL), intArrayOf(7)) {
            override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {}
        }

        val connections = arrayListOf(PortConnection(this, component.getPort(0), "7-bit input", width / 2, height))
        init(component, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        val value = circuitState!!.getLastReceived(component.getPort(0))

        val x = screenX
        val y = screenY
        val width = screenWidth
        val height = screenHeight

        val margin = 4.0
        val size = 6.0

        graphics.fill = if (value.getBit(0) == WireValue.State.ONE) Color.RED else Color.LIGHTGRAY
        graphics.fillRect(x + margin + size, y + margin, width - 2 * margin - 2 * size, size)

        graphics.fill = if (value.getBit(1) == WireValue.State.ONE) Color.RED else Color.LIGHTGRAY
        graphics.fillRect(x + margin + size, y + (height - size) / 2.0, width - 2 * margin - 2 * size, size)

        graphics.fill = if (value.getBit(2) == WireValue.State.ONE) Color.RED else Color.LIGHTGRAY
        graphics.fillRect(x + margin + size, y + height - margin - size, width - 2 * margin - 2 * size, size)

        graphics.fill = if (value.getBit(3) == WireValue.State.ONE) Color.RED else Color.LIGHTGRAY
        graphics.fillRect(x + width - margin - size, y + margin + size / 2.0, size, (height - size) / 2.0 - margin)

        graphics.fill = if (value.getBit(4) == WireValue.State.ONE) Color.RED else Color.LIGHTGRAY
        graphics.fillRect(x + margin, y + margin + size / 2.0, size, (height - size) / 2.0 - margin)

        graphics.fill = if (value.getBit(5) == WireValue.State.ONE) Color.RED else Color.LIGHTGRAY
        graphics.fillRect(x + width - margin - size, y + height / 2.0, size, (height - size) / 2.0 - margin)

        graphics.fill = if (value.getBit(6) == WireValue.State.ONE) Color.RED else Color.LIGHTGRAY
        graphics.fillRect(x + margin, y + height / 2.0, size, (height - size) / 2.0 - margin)
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Input/Output", "7-Segment Display"),
                Image(SevenSegmentDisplay::class.java.getResourceAsStream("/images/HexDisplay.png")),
                Properties(), true
            )
        }
    }
}
