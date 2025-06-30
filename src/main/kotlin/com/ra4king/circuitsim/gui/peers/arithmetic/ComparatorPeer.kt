package com.ra4king.circuitsim.gui.peers.arithmetic

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.Properties.*
import com.ra4king.circuitsim.gui.properties.PropertyListValidator
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.arithmetic.Comparator
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class ComparatorPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Comparator>(x, y, 4, 4) {
    init {
        val properties = Properties()
        properties.ensureProperty(LABEL)
        properties.ensureProperty(LABEL_LOCATION)
        properties.ensureProperty(BITSIZE)
        properties.ensureProperty(USE_SIGNED_COMPARE)
        properties.mergeIfExists(props)

        val comparator = Comparator(
            properties.getValue(LABEL),
            properties.getValue(BITSIZE),
            properties.getValue(USE_SIGNED_COMPARE)
        )

        val connections = arrayListOf(
            PortConnection(this, comparator.getPort(Comparator.Ports.PORT_A), "A", 0, 1),
            PortConnection(this, comparator.getPort(Comparator.Ports.PORT_B), "B", 0, 3),
            PortConnection(this, comparator.getPort(Comparator.Ports.PORT_LT), "A < B", width, 1),
            PortConnection(this, comparator.getPort(Comparator.Ports.PORT_EQ), "A = B", width, 2),
            PortConnection(this, comparator.getPort(Comparator.Ports.PORT_GT), "A > B", width, 3)
        )

        init(comparator, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        graphics.font = getFont(12, true)
        graphics.fill = Color.BLACK
        graphics.fillText("<", screenX + screenWidth - 12.0, (screenY + 12).toDouble())
        graphics.fillText("=", screenX + screenWidth - 12.0, (screenY + 24).toDouble())
        graphics.fillText(">", screenX + screenWidth - 12.0, (screenY + 35).toDouble())
    }

    companion object {
        private val USE_SIGNED_COMPARE = Property(
            "Comparison Type",
            PropertyListValidator<Boolean>(
                mutableListOf(true, false)
            ) { if (it) "2's " + "complement" else "Unsigned" },
            true
        )

        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Arithmetic", "Comparator"),
                Image(ComparatorPeer::class.java.getResourceAsStream("/images/Comparator.png")),
                Properties(), true
            )
        }
    }
}
