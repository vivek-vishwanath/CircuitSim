package com.ra4king.circuitsim.gui.peers.wiring

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.setBitColor
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.components.wiring.Power
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image


/**
 * @author Austin Adams and Roi Atalla
 */
class PowerPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Power>(x, y, 2, 3) {
    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.mergeIfExists(props)

        val power = Power(properties.getValue(Properties.LABEL))

        val connections = arrayListOf(PortConnection(this, power.getPort(0), width / 2, height))

        init(power, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))

        val x = screenX
        val y = screenY
        val width = screenWidth
        val height = screenHeight

        setBitColor(graphics, WireValue.State.ONE)

        graphics.lineWidth = 2.0

        graphics.beginPath()
        graphics.moveTo(x.toDouble(), y.toDouble())
        graphics.lineTo((x + width).toDouble(), y.toDouble())
        graphics.moveTo(x + 0.5 * width, y.toDouble())
        graphics.lineTo(x + 0.5 * width, (y + height).toDouble())
        graphics.stroke()
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Wiring", "Power"),
                Image(PowerPeer::class.java.getResourceAsStream("/images/Power.png")),
                Properties(), true
            )
        }
    }
}
