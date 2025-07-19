package com.ra4king.circuitsim.gui.peers.arithmetic

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.Properties.*
import com.ra4king.circuitsim.gui.properties.PropertyListValidator
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.arithmetic.Shifter
import com.ra4king.circuitsim.simulator.components.arithmetic.Shifter.ShiftType
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color

/**
 * @author Roi Atalla
 */
class ShifterPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Shifter>(x, y, 4, 4) {
    init {
        val properties = Properties()
        properties.ensureProperty(LABEL)
        properties.ensureProperty(LABEL_LOCATION)
        properties.ensureProperty(BITSIZE)
        properties.ensureProperty(
            Property(
                "Shift Type",
                PropertyListValidator(ShiftType.entries.toTypedArray())
                { it.toString().replace('_', ' ') },
                ShiftType.LOGICAL_LEFT
            )
        )
        properties.mergeIfExists(props)

        val shifter = Shifter(
            properties.getValue(LABEL),
            properties.getValue(BITSIZE),
            properties.getValue("Shift Type")
        )

        val connections = arrayListOf(
            PortConnection(this, shifter.getPort(Shifter.Ports.PORT_IN), "In", 0, 1),
            PortConnection(this, shifter.getPort(Shifter.Ports.PORT_SHIFT), "Shift", 0, 3),
            PortConnection(this, shifter.getPort(Shifter.Ports.PORT_OUT), "Out", 4, 2),
        )

        init(shifter, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        graphics.fill = Color.BLACK
        graphics.lineWidth = 1.5

        val x = screenX
        val y = screenY
        val width = screenWidth
        val height = screenHeight

        fun rotateSymbol(right: Boolean) {
            graphics.strokeLine(x + width - if (right) 15.0 else 5.0, y + height * 0.5, x + width - if (right) 15.0 else 5.0, y + height * 0.5 - 10)
            graphics.strokeLine(x + width - 15.0, y + height * 0.5 - 10, x + width - 5.0, y + height * 0.5 - 10)
        }

        fun leftArrow() {
            graphics.strokeLine(x + width - 15.0,y + height * 0.5, x + width - 5.0, y + height * 0.5)
            graphics.strokeLine(x + width - 10.0,y + height * 0.5 - 5, x + width - 15.0, y + height * 0.5)
            graphics.strokeLine(x + width - 10.0,y + height * 0.5 + 5, x + width - 15.0, y + height * 0.5)
        }

        fun rightArrow() {
            graphics.strokeLine(x + width - 15.0, y + height * 0.5, x + width - 5.0, y + height * 0.5)
            graphics.strokeLine(x + width - 10.0, y + height * 0.5 - 5, x + width - 5.0, y + height * 0.5)
            graphics.strokeLine(x + width - 10.0, y + height * 0.5 + 5, x + width - 5.0, y + height * 0.5)
        }

        val shiftType = properties.getValue<ShiftType>("Shift Type")
        when (shiftType) {
            ShiftType.ROTATE_LEFT -> {
                rotateSymbol(false)
                leftArrow()
            }

            ShiftType.LOGICAL_LEFT -> leftArrow()

            ShiftType.ROTATE_RIGHT -> {
                rotateSymbol(true)
                rightArrow()
            }

            ShiftType.ARITHMETIC_RIGHT -> {
                graphics.strokeLine(x + width - 20.0,y + height * 0.5, x + width - 18.0,y + height * 0.5)
                rightArrow()
            }

            ShiftType.LOGICAL_RIGHT -> rightArrow()
        }
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Arithmetic", "Shifter"),
                Image(ShifterPeer::class.java.getResourceAsStream("/images/Shifter.png")),
                Properties(), true
            )
        }
    }
}
