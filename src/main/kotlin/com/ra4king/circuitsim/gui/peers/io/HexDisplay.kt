package com.ra4king.circuitsim.gui.peers.io

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class HexDisplay(props: Properties, x: Int, y: Int) : ComponentPeer<Component>(x, y, 4, 6) {
    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        drawDigit(graphics, -1)

        val value = circuitState!!.getLastReceived(component.getPort(0))
        if (value.isValidValue) {
            drawDigit(graphics, value.value)
        }
    }

    private fun drawDigit(graphics: GraphicsContext, num: Int) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))

        val x = screenX
        val y = screenY
        val width = screenWidth
        val height = screenHeight

        val margin = 4.0
        val size = 6.0

        graphics.fill = if (top.contains(num)) Color.RED else Color.LIGHTGRAY
        graphics.fillRect(
            (x + margin + size),
            (y + margin),
            (width - 2 * margin - 2 * size),
            size
        )

        graphics.fill = if (middle.contains(num)) Color.RED else Color.LIGHTGRAY
        graphics.fillRect(
            (x + margin + size),
            y + (height - size) / 2.0,
            (width - 2 * margin - 2 * size),
            size
        )

        graphics.fill = if (bottom.contains(num)) Color.RED else Color.LIGHTGRAY
        graphics.fillRect(
            (x + margin + size),
            (y + height - margin - size),
            (width - 2 * margin - 2 * size),
            size
        )

        graphics.fill = if (topRight.contains(num)) Color.RED else Color.LIGHTGRAY
        graphics.fillRect(
            (x + width - margin - size),
            y + margin + size / 2.0,
            size,
            (height - size) / 2.0 - margin
        )

        graphics.fill = if (topLeft.contains(num)) Color.RED else Color.LIGHTGRAY
        graphics.fillRect(
            (x + margin),
            y + margin + size / 2.0,
            size,
            (height - size) / 2.0 - margin
        )

        graphics.fill = if (botRight.contains(num)) Color.RED else Color.LIGHTGRAY
        graphics.fillRect(
            (x + width - margin - size),
            y + height / 2.0,
            size,
            (height - size) / 2.0 - margin
        )

        graphics.fill = if (botLeft.contains(num)) Color.RED else Color.LIGHTGRAY
        graphics.fillRect((x + margin), y + height / 2.0, size, (height - size) / 2.0 - margin)
    }

    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.mergeIfExists(props)

        val component: Component = object : Component(properties.getValue(Properties.LABEL), intArrayOf(4)) {
            override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {}
        }

        val connections = arrayListOf(PortConnection(this, component.getPort(0), "4-bit input", width / 2, height))
        init(component, properties, connections)
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Input/Output", "Hex Display"),
                Image(HexDisplay::class.java.getResourceAsStream("/images/HexDisplay.png")),
                Properties(), true
            )
        }

        private val top = HashSet(mutableListOf(0, 2, 3, 5, 6, 7, 8, 9, 10, 12, 14, 15))
        private val topRight = HashSet(mutableListOf(0, 1, 2, 3, 4, 7, 8, 9, 10, 13))
        private val botRight =
            HashSet(mutableListOf(0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13))
        private val bottom = HashSet(mutableListOf(0, 2, 3, 5, 6, 8, 9, 11, 12, 13, 14))
        private val botLeft = HashSet(mutableListOf(0, 2, 6, 8, 10, 11, 12, 13, 14, 15))
        private val topLeft = HashSet(mutableListOf(0, 4, 5, 6, 8, 9, 10, 11, 12, 14, 15))
        private val middle =
            HashSet(mutableListOf(2, 3, 4, 5, 6, 8, 9, 10, 11, 13, 14, 15))
    }
}
