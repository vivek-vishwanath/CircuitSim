package com.ra4king.circuitsim.gui.peers.wiring

import com.ra4king.circuitsim.gui.CircuitManager
import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.drawShape
import com.ra4king.circuitsim.gui.GuiUtils.setBitColor
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.wiring.Clock
import com.ra4king.circuitsim.simulator.components.wiring.Clock.Companion.getTickState
import com.ra4king.circuitsim.simulator.components.wiring.Clock.Companion.tick
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color

/**
 * @author Roi Atalla
 */
class ClockPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Clock>(x, y, 2, 2) {
    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.DIRECTION)
        properties.mergeIfExists(props)

        val clock = Clock(properties.getValue(Properties.LABEL))

        val connections = ArrayList<PortConnection>()
        when (properties.getValue(Properties.DIRECTION)) {
            Properties.Direction.EAST -> connections.add(PortConnection(this,clock.getPort(Clock.PORT),width,height / 2))
            Properties.Direction.WEST -> connections.add(PortConnection(this, clock.getPort(Clock.PORT), 0, height / 2))
            Properties.Direction.NORTH -> connections.add(PortConnection(this, clock.getPort(Clock.PORT), width / 2, 0))
            Properties.Direction.SOUTH -> connections.add(PortConnection(this,clock.getPort(Clock.PORT),width / 2,height)
            )
        }

        init(clock, properties, connections)
    }

    override fun mousePressed(manager: CircuitManager, state: CircuitState, x: Double, y: Double) {
        tick(component.circuit!!.simulator)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))

        val port = component.getPort(Clock.PORT)
        if (circuitState!!.isShortCircuited(port.link)) {
            graphics.fill = Color.RED
        } else {
            setBitColor(graphics, circuitState.getLastPushed(port).getBit(0))
        }
        drawShape(this) { v: Double, v1: Double, v2: Double, v3: Double -> graphics.fillRect(v, v1, v2, v3) }
        graphics.stroke = Color.BLACK
        drawShape(this) { v: Double, v1: Double, v2: Double, v3: Double -> graphics.strokeRect(v, v1, v2, v3) }

        graphics.stroke = Color.WHITE
        graphics.lineWidth = 1.5
        val offset1 = if (getTickState(component.circuit!!.simulator)) 0.3 else 0.0
        val offset2 = if (getTickState(component.circuit!!.simulator)) 0.6 else 0.0


        // lower line
        graphics.strokeLine(
            screenX + screenWidth * (0.2 + offset1),
            screenY + screenHeight * 0.7,
            screenX + screenWidth * (0.5 + offset1),
            screenY + screenHeight * 0.7
        )


        // upper line
        graphics.strokeLine(
            screenX + screenWidth * (0.5 - offset1),
            screenY + screenHeight * 0.3,
            screenX + screenWidth * (0.8 - offset1),
            screenY + screenHeight * 0.3
        )


        // lower vertical line
        graphics.strokeLine(
            screenX + screenWidth * (0.2 + offset2),
            screenY + screenHeight * 0.5,
            screenX + screenWidth * (0.2 + offset2),
            screenY + screenHeight * 0.7
        )


        // upper vertical  line
        graphics.strokeLine(
            screenX + screenWidth * (0.8 - offset2),
            screenY + screenHeight * 0.3,
            screenX + screenWidth * (0.8 - offset2),
            screenY + screenHeight * 0.5
        )


        // middle vertical line
        graphics.strokeLine(
            screenX + screenWidth * 0.5,
            screenY + screenHeight * 0.3,
            screenX + screenWidth * 0.5,
            screenY + screenHeight * 0.7
        )
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Wiring", "Clock"),
                Image(ClockPeer::class.java.getResourceAsStream("/images/Clock.png")),
                Properties(), true
            )
        }
    }
}
