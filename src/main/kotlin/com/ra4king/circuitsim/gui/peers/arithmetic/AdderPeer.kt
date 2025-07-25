package com.ra4king.circuitsim.gui.peers.arithmetic

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.getBounds
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.Properties.*
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.arithmetic.Adder
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class AdderPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Adder>(x, y, 4, 4) {
    init {
        val properties = Properties()
        properties.ensureProperty(LABEL)
        properties.ensureProperty(LABEL_LOCATION)
        properties.ensureProperty(BITSIZE)
        properties.mergeIfExists(props)

        val adder = Adder(properties.getValue(LABEL), properties.getValue(BITSIZE))

        val connections = arrayListOf(
            PortConnection(this, adder.getPort(Adder.Ports.PORT_A), "A", 0, 1),
            PortConnection(this, adder.getPort(Adder.Ports.PORT_B), "B", 0, 3),
            PortConnection(this, adder.getPort(Adder.Ports.PORT_CARRY_IN), "Carry in", 2, 0),
            PortConnection(this, adder.getPort(Adder.Ports.PORT_OUT), "Out", width, 2),
            PortConnection(this, adder.getPort(Adder.Ports.PORT_CARRY_OUT), "Carry out", 2, height)
        )

        init(adder, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        graphics.font = getFont(16, true)
        val bounds = getBounds(graphics.font, "+")
        graphics.fill = Color.BLACK
        graphics.fillText(
            "+",
            screenX + (screenWidth - bounds.width) * 0.5,
            screenY + (screenHeight + bounds.height) * 0.45
        )
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Arithmetic", "Adder"),
                Image(AdderPeer::class.java.getResourceAsStream("/images/Adder.png")),
                Properties(), true
            )
        }
    }
}
