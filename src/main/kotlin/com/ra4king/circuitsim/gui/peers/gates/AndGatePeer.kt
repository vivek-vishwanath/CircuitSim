package com.ra4king.circuitsim.gui.peers.gates

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.GuiUtils
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.gates.Gate.AndGate
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class AndGatePeer(properties: Properties, x: Int, y: Int) : GatePeer<AndGate>(properties, x, y) {
    override fun ensureProperties(properties: Properties) {
        properties.ensureProperty(Properties.BITSIZE)
        properties.ensureProperty(Properties.NUM_INPUTS)
    }

    override fun buildGate(properties: Properties): AndGate {
        val numInputs: Int
        return AndGate(
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
        graphics.moveTo(x, y)
        graphics.lineTo(x, (y + height))
        graphics.arc(x + width * 0.5, y + height * 0.5, width * 0.5, height * 0.5, 270.0, 180.0)
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
                Pair("Gates", "AND"),
                Image(AndGatePeer::class.java.getResourceAsStream("/images/AndGate.png")),
                Properties(), true
            )
        }
    }
}
