package com.ra4king.circuitsim.gui.peers.wiring

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.setBitColor
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.components.wiring.Ground
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image


/**
 * @author Austin Adams and Roi Atalla
 */
class GroundPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Ground>(x, y, 2, 3) {
    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.mergeIfExists(props)

        val ground = Ground(properties.getValue(Properties.LABEL))

        val connections = arrayListOf(PortConnection(this, ground.getPort(0), width / 2, 0))

        init(ground, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))

        val x = screenX
        val y = screenY
        val width = screenWidth
        val height = screenHeight

        setBitColor(graphics, WireValue.State.ZERO)

        graphics.lineWidth = 2.0

        graphics.beginPath()
        graphics.moveTo(x + 0.5 * width, y.toDouble())
        graphics.lineTo(x + 0.5 * width, y + 0.5 * height)
        graphics.lineTo(x.toDouble(), y + 0.5 * height)
        graphics.lineTo(x + 0.5 * width, (y + height).toDouble())
        graphics.lineTo((x + width).toDouble(), y + 0.5 * height)
        graphics.lineTo(x.toDouble(), y + 0.5 * height)
        graphics.stroke()
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Wiring", "Ground"),
                Image(GroundPeer::class.java.getResourceAsStream("/images/Ground.png")),
                Properties(), true
            )
        }
    }
}
