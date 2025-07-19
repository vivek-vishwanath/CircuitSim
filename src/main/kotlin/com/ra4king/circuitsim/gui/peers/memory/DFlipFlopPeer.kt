package com.ra4king.circuitsim.gui.peers.memory

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.drawClockInput
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.GuiUtils.setBitColor
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.memory.DFlipFlop
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class DFlipFlopPeer(props: Properties, x: Int, y: Int) : ComponentPeer<DFlipFlop>(x, y, 4, 4) {
    private val clockConnection: PortConnection

    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.mergeIfExists(props)

        val flipFlop = DFlipFlop(properties.getValue(Properties.LABEL))

        val connections = arrayListOf(
            PortConnection(this, flipFlop.getPort(DFlipFlop.Ports.PORT_CLOCK), "Clock", 0, 1),
            PortConnection(this, flipFlop.getPort(DFlipFlop.Ports.PORT_D), "Data input", 0, 3),
            PortConnection(this, flipFlop.getPort(DFlipFlop.Ports.PORT_PRESET), "Preset", 1, 4),
            PortConnection(this, flipFlop.getPort(DFlipFlop.Ports.PORT_ENABLE), "Enable", 2, 4),
            PortConnection(this, flipFlop.getPort(DFlipFlop.Ports.PORT_CLEAR), "Clear", 3, 4),
            PortConnection(this, flipFlop.getPort(DFlipFlop.Ports.PORT_Q), "Current state", 4, 1),
            PortConnection(this, flipFlop.getPort(DFlipFlop.Ports.PORT_QN), "NOT of current state", 4, 3)
        )
        clockConnection = connections[0]

        init(flipFlop, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        val x = screenX.toDouble()
        val y = screenY.toDouble()
        val width = screenWidth
        val height = screenHeight

        val bit = circuitState!!.getLastPushed(component.getPort(DFlipFlop.Ports.PORT_Q)).getBit(0)
        setBitColor(graphics, bit)
        graphics.fillOval(x + width * 0.5 - 10, y + height * 0.5 - 10, 20.0, 20.0)

        graphics.fill = Color.WHITE
        graphics.font = getFont(16)
        graphics.fillText(bit.repr.toString(), x + width * 0.5 - 5, y + height * 0.5 + 6)

        graphics.fill = Color.GRAY
        graphics.font = getFont(10)
        graphics.fillText("D",x + 3,y + height - 7)
        graphics.fillText("Q",x + width - 10,y + 13)
        graphics.fillText("1",x + 7,y + height - 4)
        graphics.fillText("en", x + width * 0.5 - 6,y + height - 4)
        graphics.fillText("0",x + width - 13,y + height - 4)

        graphics.stroke = Color.BLACK
        drawClockInput(graphics, clockConnection, Properties.Direction.WEST)
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Memory", "D Flip-Flop"),
                Image(DFlipFlopPeer::class.java.getResourceAsStream("/images/DFlipFlop.png")),
                Properties(),  // Confusing to 2110 students since it has both an "enable" pin and a
                // clock pin. Also, it outputs the new value half a cycle sooner than
                // the design shown in class. We have also never used this
                // component in an assignment and never will, to my knowledge. So
                // hide it from the list of components.
                false
            )
        }
    }
}
