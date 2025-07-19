package com.ra4king.circuitsim.gui.peers.gates

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.rotateGraphics
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.gates.ControlledBuffer
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class ControlledBufferPeer(props: Properties, x: Int, y: Int) : ComponentPeer<ControlledBuffer>(x, y, 2, 2) {
    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.DIRECTION)
        properties.ensureProperty(Properties.BITSIZE)
        properties.mergeIfExists(props)

        val buffer =
            ControlledBuffer(
                properties.getValue(Properties.LABEL),
                properties.getValue(Properties.BITSIZE)
            )

        val connections = ArrayList<PortConnection>()
        connections.add(
            PortConnection(
                this,
                buffer.getPort(ControlledBuffer.Ports.PORT_IN),
                "In",
                0,
                height / 2
            )
        )
        connections.add(
            PortConnection(
                this,
                buffer.getPort(ControlledBuffer.Ports.PORT_ENABLE),
                "Enable",
                width / 2,
                height
            )
        )
        connections.add(
            PortConnection(
                this, buffer.getPort(ControlledBuffer.Ports.PORT_OUT),
                "Out",
                width,
                height / 2
            )
        )
        GuiUtils.rotatePorts(
            connections, Properties.Direction.EAST, properties.getValue(
                Properties.DIRECTION
            )
        )

        init(buffer, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))
        rotateGraphics(this, graphics, properties.getValue(Properties.DIRECTION))

        val x = screenX.toDouble()
        val y = screenY.toDouble()

        graphics.beginPath()
        graphics.moveTo(x, y)
        graphics.lineTo(x + screenWidth, y + screenHeight * 0.5)
        graphics.lineTo(x, y + screenHeight)
        graphics.closePath()

        graphics.fill = Color.WHITE
        graphics.fill()
        graphics.stroke = Color.BLACK
        graphics.stroke()
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Gates", "Buffer"),
                Image(ControlledBufferPeer::class.java.getResourceAsStream("/images/Buffer.png")),
                Properties(), true
            )
        }
    }
}
