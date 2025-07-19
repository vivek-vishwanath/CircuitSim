package com.ra4king.circuitsim.gui.peers.arithmetic

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.getBounds
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.Properties.*
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.arithmetic.Multiplier
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class MultiplierPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Multiplier>(x, y, 4, 4) {
    init {
        val properties = Properties()
        properties.ensureProperty(LABEL)
        properties.ensureProperty(LABEL_LOCATION)
        properties.ensureProperty(BITSIZE)
        properties.mergeIfExists(props)

        val multiplier = Multiplier(properties.getValue(LABEL), properties.getValue(BITSIZE))

        val connections = arrayListOf(
            PortConnection(this, multiplier.getPort(Multiplier.Ports.PORT_A), "A", 0, 1),
            PortConnection(this, multiplier.getPort(Multiplier.Ports.PORT_B), "B", 0, 3),
            PortConnection(this, multiplier.getPort(Multiplier.Ports.PORT_CARRY_IN), "Carry in", 2, 0),
            PortConnection(this, multiplier.getPort(Multiplier.Ports.PORT_OUT_LOWER), "Out", width, 2),
            PortConnection(this, multiplier.getPort(Multiplier.Ports.PORT_OUT_UPPER), "Upper bits", 2, height)
        )

        init(multiplier, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        graphics.font = getFont(16, true)
        val bounds = getBounds(graphics.font, "×")
        graphics.fill = Color.BLACK
        graphics.fillText(
            "×",
            screenX + (screenWidth - bounds.width) * 0.5,
            screenY + (screenHeight + bounds.height) * 0.45
        )
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Arithmetic", "Multiplier"),
                Image(MultiplierPeer::class.java.getResourceAsStream("/images/Multiplier.png")),
                Properties(), true
            )
        }
    }
}
