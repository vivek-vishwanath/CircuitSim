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
import com.ra4king.circuitsim.simulator.components.arithmetic.Negator
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class NegatorPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Negator>(x, y, 4, 4) {
    init {
        val properties = Properties()
        properties.ensureProperty(LABEL)
        properties.ensureProperty(LABEL_LOCATION)
        properties.ensureProperty(BITSIZE)
        properties.mergeIfExists(props)

        val negator = Negator(properties.getValue(LABEL), properties.getValue(BITSIZE))

        val connections = arrayListOf(
            PortConnection(this, negator.getPort(Negator.Ports.PORT_IN), "In", 0, 2),
            PortConnection(this, negator.getPort(Negator.Ports.PORT_OUT), "Out", 4, 2)
        )

        init(negator, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        graphics.font = getFont(16, true)
        val bounds = getBounds(graphics.font, "-x")
        graphics.fill = Color.BLACK
        graphics.fillText(
            "-x",
            screenX + (screenWidth - bounds.width) * 0.5,
            screenY + (screenHeight + bounds.height) * 0.45
        )
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Arithmetic", "Negator"),
                Image(NegatorPeer::class.java.getResourceAsStream("/images/Negator.png")),
                Properties(), true
            )
        }
    }
}
