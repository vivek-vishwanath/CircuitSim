package com.ra4king.circuitsim.gui.peers.io

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.drawShape
import com.ra4king.circuitsim.gui.GuiUtils.setBitColor
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import java.util.*

/**
 * @author Roi Atalla
 */
class LEDMatrix(props: Properties, x: Int, y: Int) : ComponentPeer<Component>(x, y, 0, 0) {
    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(COL_COUNT)
        properties.ensureProperty(ROW_COUNT)
        properties.mergeIfExists(props)

        val rows: Int = properties.getValue(ROW_COUNT)
        val cols: Int = properties.getValue(COL_COUNT)

        width = cols
        height = rows

        val bitsizes = IntArray(rows)
        Arrays.fill(bitsizes, cols)

        val component: Component = object : Component(properties.getValue(Properties.LABEL), bitsizes) {
            override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {}
        }

        val connections = Array(rows) { PortConnection(this, component.getPort(it), 0, it) }.toCollection(ArrayList())

        init(component, properties, connections)
    }

    override val screenY: Int
        get() = (super.screenY - 0.5 * GuiUtils.BLOCK_SIZE).toInt()

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))

        for (i in 0..<component.numPorts) {
            val value = circuitState!!.getLastReceived(component.getPort(i))
            for (b in value.bitSize - 1 downTo 0) {
                setBitColor(graphics, value.getBit(b))
                graphics.fillRect(
                    (screenX + (value.bitSize - b - 1) * GuiUtils.BLOCK_SIZE).toDouble(),
                    (screenY + i * GuiUtils.BLOCK_SIZE).toDouble(),
                    GuiUtils.BLOCK_SIZE.toDouble(),
                    GuiUtils.BLOCK_SIZE.toDouble()
                )
            }
        }

        graphics.stroke = Color.BLACK
        drawShape(this) { v: Double, v1: Double, v2: Double, v3: Double -> graphics.strokeRect(v, v1, v2, v3) }
    }

    companion object {
        private val COL_COUNT = Properties.Property("Column count", Properties.BITSIZE.validator, 5)
        private val ROW_COUNT = Properties.Property("Row count", Properties.BITSIZE.validator, 7)

        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Input/Output", "LED Matrix"),
                Image(LEDMatrix::class.java.getResourceAsStream("/images/LEDMatrix.png")),
                Properties(),
                true
            )
        }
    }
}
