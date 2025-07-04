package com.ra4king.circuitsim.gui.peers.wiring

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.rotateElementSize
import com.ra4king.circuitsim.gui.GuiUtils.rotateGraphics
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.properties.PropertyListValidator
import com.ra4king.circuitsim.gui.properties.PropertyValidators
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.wiring.Transistor
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import kotlin.math.max
import kotlin.math.min

/**
 * @author Roi Atalla
 */
class TransistorPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Transistor>(x, y, 4, 2) {
    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.DIRECTION)
        properties.ensureProperty(TRANSISTOR_TYPE_PROPERTY)
        properties.ensureProperty(GATE_LOCATION_PROPERTY)
        properties.mergeIfExists(props)

        val transistor = Transistor(
            properties.getValue(Properties.LABEL), properties.getValue(
                TRANSISTOR_TYPE_PROPERTY
            )
        )



        val yOff = when (properties.getValue(Properties.DIRECTION)) {
            Properties.Direction.EAST, Properties.Direction.NORTH -> if (properties.getValue(GATE_LOCATION_PROPERTY)) 0 else height
            Properties.Direction.WEST, Properties.Direction.SOUTH -> if (properties.getValue(GATE_LOCATION_PROPERTY)) height else 0
        }

        val connections = arrayListOf(
            PortConnection(this, transistor.getPort(Transistor.Ports.PORT_IN),"Input",0,height - yOff),
            PortConnection(this,transistor.getPort(Transistor.Ports.PORT_GATE),"Gate",width / 2,yOff),
            PortConnection(this,transistor.getPort(Transistor.Ports.PORT_OUT),"Output",width,height - yOff))

        GuiUtils.rotatePorts(connections, Properties.Direction.EAST, properties.getValue(Properties.DIRECTION))
        rotateElementSize(
            this,
            Properties.Direction.EAST,
            properties.getValue(Properties.DIRECTION)
        )

        init(transistor, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))
        rotateGraphics(this, graphics, properties.getValue(Properties.DIRECTION))

        val x = screenX.toDouble()
        val y = screenY.toDouble()
        val width = max(screenWidth, screenHeight)
        val height = min(screenWidth, screenHeight)

        var gateLoc: Boolean = properties.getValue(GATE_LOCATION_PROPERTY)
        when (properties.getValue(Properties.DIRECTION)) {
            Properties.Direction.WEST, Properties.Direction.SOUTH -> gateLoc = !gateLoc
            Properties.Direction.EAST, Properties.Direction.NORTH -> {}
        }

        val yOff = if (gateLoc) 0 else height
        val m = if (gateLoc) 1 else -1

        graphics.stroke = Color.BLACK
        graphics.lineWidth = 2.0

        graphics.beginPath()
        graphics.moveTo(x, y + height - yOff)
        graphics.lineTo(x + width / 3.0, y + height - yOff)
        graphics.lineTo(x + width / 3.0, y + yOff + m * height * 0.7)
        graphics.lineTo(x + 2.0 * width / 3.0, y + yOff + m * height * 0.7)
        graphics.lineTo(x + 2.0 * width / 3.0, y + height - yOff)
        graphics.lineTo(x + width, y + height - yOff)

        graphics.moveTo(x + width / 3.0, y + yOff + m * height * 0.5)
        graphics.lineTo(x + 2.0 * width / 3.0, y + yOff + m * height * 0.5)
        graphics.stroke()

        graphics.lineWidth = 1.0

        graphics.beginPath()
        graphics.moveTo(x + width * 0.5 - 1.5, y + yOff + m * (height * 0.7 + 3))
        graphics.lineTo(x + width * 0.5 + 3.0, y + yOff + m * (height * 0.7 + 5))
        graphics.lineTo(x + width * 0.5 - 1.5, y + yOff + m * (height * 0.7 + 7))
        graphics.stroke()

        if (properties.getValue(TRANSISTOR_TYPE_PROPERTY)) {
            graphics.strokeOval(x + width * 0.5 - 3, y + (if (gateLoc) 3 else height - 9), 6.0, 6.0)
        } else {
            graphics.strokeLine(x + width * 0.5, y + yOff, x + width * 0.5, y + height * 0.5)
        }
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Wiring", "Transistor (Deprecated)"),
                Image(TransistorPeer::class.java.getResourceAsStream("/images/Transistor.png")),
                Properties(),  // Deprecated
                false
            )
        }

        private val TRANSISTOR_TYPE_PROPERTY =
            Properties.Property(
                "Type",
                PropertyListValidator(
                    mutableListOf(true, false)
                ) { `val` -> if (`val`) "P-Type" else "N-Type" },
                true
            )

        private val GATE_LOCATION_PROPERTY =
            Properties.Property("Gate Location", PropertyValidators.LOCATION_VALIDATOR, true)
    }
}
