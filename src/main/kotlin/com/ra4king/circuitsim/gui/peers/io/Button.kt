package com.ra4king.circuitsim.gui.peers.io

import com.ra4king.circuitsim.gui.CircuitManager
import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class Button(props: Properties, x: Int, y: Int) : ComponentPeer<Component>(x, y, 2, 2) {
    private var isPressed = false

    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.DIRECTION)
        properties.mergeIfExists(props)

        val component: Component = object : Component(properties.getValue(Properties.LABEL), intArrayOf(1)) {
            override fun init(circuitState: CircuitState, lastProperty: Any?) {
                circuitState.pushValue(getPort(0), of(0, 1))
            }

            override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {}
        }

        val connections = arrayListOf(PortConnection(this, component.getPort(0), width, height / 2))

        GuiUtils.rotatePorts(
            connections, Properties.Direction.EAST, properties.getValue(
                Properties.DIRECTION
            )
        )

        init(component, properties, connections)
    }

    override fun mousePressed(manager: CircuitManager, state: CircuitState, x: Double, y: Double) {
        isPressed = true
        state.pushValue(component.getPort(0), of(1, 1))
    }

    override fun mouseReleased(manager: CircuitManager, state: CircuitState, x: Double, y: Double) {
        isPressed = false
        state.pushValue(component.getPort(0), of(0, 1))
    }

    override fun keyPressed(
        manager: CircuitManager,
        state: CircuitState,
        keyCode: KeyCode,
        text: String
    ): Boolean {
        if (keyCode == KeyCode.SPACE) {
            mousePressed(manager, state, 0.0, 0.0)
        }

        return false
    }

    override fun keyReleased(manager: CircuitManager, state: CircuitState, keyCode: KeyCode, text: String) {
        if (keyCode == KeyCode.SPACE) {
            mouseReleased(manager, state, 0.0, 0.0)
        }
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))

        val x = screenX.toDouble()
        val y = screenY.toDouble()
        val width = screenWidth
        val height = screenHeight
        val offset = 3.0

        graphics.stroke = Color.BLACK
        graphics.fill = if (isPressed) Color.WHITE else Color.DARKGRAY

        graphics.fillRect((x + offset),(y + offset),(width - offset),(height - offset))
        graphics.strokeRect((x + offset),(y + offset),(width - offset),(height - offset))

        if (!isPressed) {
            graphics.fill = Color.WHITE

            graphics.fillRect(x, y, (width - offset), (height - offset))
            graphics.strokeRect(x, y, (width - offset), (height - offset))

            graphics.strokeLine(x,(y + height - offset),(x + offset),(y + height))
            graphics.strokeLine((x + width - offset),y,(x + width),(y + offset))
            graphics.strokeLine((x + width - offset),(y + height - offset),(x + width),(y + height))
        }
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Input/Output", "Button"),
                Image(Button::class.java.getResourceAsStream("/images/Button.png")),
                Properties(), true
            )
        }
    }
}
