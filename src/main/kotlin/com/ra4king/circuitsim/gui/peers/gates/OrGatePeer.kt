package com.ra4king.circuitsim.gui.peers.gates

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.GuiUtils
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.gates.Gate.OrGate
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class OrGatePeer(properties: Properties, x: Int, y: Int) : GatePeer<OrGate>(properties, x, y) {
    override fun ensureProperties(properties: Properties) {
        properties.ensureProperty(Properties.BITSIZE)
        properties.ensureProperty(Properties.NUM_INPUTS)
    }

    override fun buildGate(properties: Properties): OrGate {
        val numInputs: Int
        return OrGate(
            properties.getValue(Properties.LABEL),
            properties.getValue(Properties.BITSIZE),
            properties.getValue(Properties.NUM_INPUTS).also { numInputs = it },
            parseNegatedInputs(numInputs, properties)
        )
    }

    override fun paintGate(graphics: GraphicsContext, circuitState: CircuitState?) {
        val x = screenX.toDouble()
        val y = screenY.toDouble()
        val width = 4.0 * GuiUtils.BLOCK_SIZE
        val height = 4.0 * GuiUtils.BLOCK_SIZE

        graphics.beginPath()
        graphics.moveTo(x, (y + height))
        graphics.arc(x, y + height * 0.5, width * 0.25, height * 0.5, 270.0, 180.0)
        graphics.arcTo(x + width * 0.66, y, x + width * 1.25, (y + height), width)
        graphics.arcTo(
            x + width * 0.66,
            (y + height),
            x,
            (y + height),
            width
        )
        graphics.closePath()

        graphics.fill = Color.WHITE
        graphics.stroke = Color.BLACK
        graphics.fill()
        graphics.stroke()
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Gates", "OR"),
                Image(OrGatePeer::class.java.getResourceAsStream("/images/OrGate.png")),
                Properties(), true
            )
        }
    }
}
