package com.ra4king.circuitsim.gui.peers.arithmetic

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.drawClockInput
import com.ra4king.circuitsim.gui.GuiUtils.getBounds
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.Properties.*
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.arithmetic.RandomGenerator
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class RandomGeneratorPeer(props: Properties, x: Int, y: Int) : ComponentPeer<RandomGenerator>(x, y, 4, 4) {
    private val clockConnection: PortConnection

    init {
        val properties = Properties()
        properties.ensureProperty(LABEL)
        properties.ensureProperty(LABEL_LOCATION)
        properties.ensureProperty(BITSIZE)
        properties.mergeIfExists(props)

        val randomGenerator = RandomGenerator(
            properties.getValue(LABEL),
            properties.getValue(BITSIZE)
        )

        val connections = arrayListOf(
            PortConnection(this, randomGenerator.getPort(RandomGenerator.Ports.PORT_CLK), "Clock", 2, 4),
            PortConnection(this, randomGenerator.getPort(RandomGenerator.Ports.PORT_OUT), "Out", 4, 2),
        )
        clockConnection = connections[0]

        init(randomGenerator, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        graphics.font = getFont(16, true)
        val bounds = getBounds(graphics.font, "RNG")
        graphics.fill = Color.BLACK
        graphics.fillText(
            "RNG",
            screenX + (screenWidth - bounds.width) * 0.5,
            screenY + (screenHeight + bounds.height) * 0.45
        )

        graphics.stroke = Color.BLACK
        drawClockInput(graphics, clockConnection, Direction.SOUTH)
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Arithmetic", "Random Generator"),
                Image(RandomGeneratorPeer::class.java.getResourceAsStream("/images/RandomGenerator.png")),
                Properties(), true
            )
        }
    }
}
