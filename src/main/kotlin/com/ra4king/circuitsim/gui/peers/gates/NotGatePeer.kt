package com.ra4king.circuitsim.gui.peers.gates

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.GuiUtils
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.gates.Gate.NotGate
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class NotGatePeer(properties: Properties, x: Int, y: Int) : GatePeer<NotGate>(properties, x, y, 3, 2, false) {
    override fun ensureProperties(properties: Properties) {
        properties.ensureProperty(Properties.BITSIZE)
    }

    override fun buildGate(properties: Properties): NotGate {
        return NotGate(properties.getValue(Properties.LABEL), properties.getValue(Properties.BITSIZE))
    }

    override fun paintGate(graphics: GraphicsContext, circuitState: CircuitState?) {
        val x = screenX
        val y = screenY
        val width = 3 * GuiUtils.BLOCK_SIZE
        val height = 2 * GuiUtils.BLOCK_SIZE

        graphics.beginPath()
        graphics.moveTo(x.toDouble(), y.toDouble())
        graphics.lineTo(x.toDouble(), (y + height).toDouble())
        graphics.lineTo(x + width * 0.7, y + height * 0.5)
        graphics.closePath()

        graphics.fill = Color.WHITE
        graphics.stroke = Color.BLACK
        graphics.fill()
        graphics.stroke()

        graphics.fillOval(x + width * 0.7, y + height * 0.5 - width * 0.125, width * 0.25, width * 0.25)
        graphics.strokeOval(x + width * 0.7, y + height * 0.5 - width * 0.125, width * 0.25, width * 0.25)
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Gates", "NOT"),
                Image(NotGatePeer::class.java.getResourceAsStream("/images/NotGate.png")),
                Properties(), true
            )
        }
    }
}
