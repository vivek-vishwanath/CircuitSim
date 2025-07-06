package com.ra4king.circuitsim.gui.peers.arithmetic

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.getBounds
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.Properties.*
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.arithmetic.Divider
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class DividerPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Divider>(x, y, 4, 4) {
    init {
        val properties = Properties()
        properties.ensureProperty(LABEL)
        properties.ensureProperty(LABEL_LOCATION)
        properties.ensureProperty(BITSIZE)
        properties.mergeIfExists(props)

        val divider = Divider(properties.getValue(LABEL), properties.getValue(BITSIZE))

        val connections = arrayListOf(
            PortConnection(this, divider.getPort(Divider.Ports.PORT_DIVIDEND), "Dividend", 0, 1),
            PortConnection(this, divider.getPort(Divider.Ports.PORT_DIVISOR), "Divisor", 0, 3),
            PortConnection(this, divider.getPort(Divider.Ports.PORT_QUOTIENT), "Quotient", width, 2),
            PortConnection(this, divider.getPort(Divider.Ports.PORT_REMAINDER), "Remainder", 2, height),
        )

        init(divider, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        graphics.font = getFont(16, true)
        val bounds = getBounds(graphics.font, "÷")
        graphics.fill = Color.BLACK
        graphics.fillText(
            "÷",
            screenX + (screenWidth - bounds.width) * 0.5,
            screenY + (screenHeight + bounds.height) * 0.45
        )
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Arithmetic", "Divider"),
                Image(DividerPeer::class.java.getResourceAsStream("/images/Divider.png")),
                Properties(), true
            )
        }
    }
}
