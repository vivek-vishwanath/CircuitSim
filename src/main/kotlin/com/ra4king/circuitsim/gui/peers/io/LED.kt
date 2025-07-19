package com.ra4king.circuitsim.gui.peers.io

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.drawShape
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.properties.PropertyValidators
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class LED(props: Properties, x: Int, y: Int) : ComponentPeer<Component>(x, y, 2, 2) {
    private val offColor: Color?
    private val onColor: Color?

    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.DIRECTION)
        properties.ensureProperty(OFF_COLOR_PROPERTY)
        properties.ensureProperty(ON_COLOR_PROPERTY)
        properties.mergeIfExists(props)

        offColor = properties.getValue(OFF_COLOR_PROPERTY)
        onColor = properties.getValue(ON_COLOR_PROPERTY)

        val component: Component = object : Component(properties.getValue(Properties.LABEL), intArrayOf(1)) {
            override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {}
        }

        val connections = arrayListOf(PortConnection(this, component.getPort(0), "1-bit input", 0, 1))

        GuiUtils.rotatePorts(
            connections, Properties.Direction.EAST, properties.getValue(
                Properties.DIRECTION
            )
        )

        init(component, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))

        val bit = circuitState!!.getLastReceived(component.getPort(0)).getBit(0)

        graphics.fill = if (bit == WireValue.State.ONE) onColor else offColor
        graphics.stroke = Color.BLACK
        drawShape(this) { v: Double, v1: Double, v2: Double, v3: Double -> graphics.fillOval(v, v1, v2, v3) }
        drawShape(this) { v: Double, v1: Double, v2: Double, v3: Double -> graphics.strokeOval(v, v1, v2, v3) }
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Input/Output", "LED"),
                Image(LED::class.java.getResourceAsStream("/images/LED.png")),
                Properties(), true
            )
        }

        private val ON_COLOR_PROPERTY =
            Properties.Property("On Color", PropertyValidators.COLOR_VALIDATOR, Color.RED)
        private val OFF_COLOR_PROPERTY =
            Properties.Property("Off Color", PropertyValidators.COLOR_VALIDATOR, Color.DARKGRAY)
    }
}
