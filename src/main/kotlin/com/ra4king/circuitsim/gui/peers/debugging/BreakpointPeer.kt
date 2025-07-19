package com.ra4king.circuitsim.gui.peers.debugging

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.getBounds
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.debugging.Breakpoint
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Charles Jenkins
 */
class BreakpointPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Breakpoint>(x, y, 4, 4) {
    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.BITSIZE)
        properties.ensureProperty(Properties.VALUE)
        properties.mergeIfExists(props)

        val breakpoint = Breakpoint(
            properties.getValue(Properties.LABEL),
            properties.getValue(Properties.BITSIZE),
            properties.getValue(Properties.VALUE).value
        )

        val connections = arrayListOf(
            PortConnection(this, breakpoint.getPort(Breakpoint.Ports.PORT_ENABLE), "ENABLE", 0, 1),
            PortConnection(this, breakpoint.getPort(Breakpoint.Ports.PORT_DATA), "DATA", 2, height),
        )

        init(breakpoint, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        graphics.font = getFont(16, true)
        val bounds = getBounds(graphics.font, "BP")
        graphics.fill = Color.BLACK
        graphics.fillText("BP",
            screenX + (screenWidth - bounds.width) * 0.5,
            screenY + (screenHeight + bounds.height) * 0.45)
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Debugging", "Breakpoint"),
                Image(BreakpointPeer::class.java.getResourceAsStream("/images/Breakpoint.png")),
                Properties(), true
            )
        }
    }
}
