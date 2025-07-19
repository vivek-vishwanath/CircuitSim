package com.ra4king.circuitsim.gui.peers.memory

import com.ra4king.circuitsim.gui.CircuitManager
import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.drawClockInput
import com.ra4king.circuitsim.gui.GuiUtils.getBounds
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import com.ra4king.circuitsim.simulator.components.memory.Register
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import kotlin.math.min

/**
 * @author Roi Atalla
 */
class RegisterPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Register>(x, y, 4, 4) {
    private val clockConnection: PortConnection

    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.BITSIZE)
        properties.mergeIfExists(props)

        val register = Register(properties.getValue(Properties.LABEL), properties.getValue(Properties.BITSIZE))

        val connections = arrayListOf(
            PortConnection(this, register.getPort(Register.Ports.PORT_IN), "In", 0, 2),
            PortConnection(this, register.getPort(Register.Ports.PORT_ENABLE), "Enable", 0, 3),
            PortConnection(this, register.getPort(Register.Ports.PORT_CLK), "Clock", 1, height),
            PortConnection(this, register.getPort(Register.Ports.PORT_ZERO), "Clear", 3, height),
            PortConnection(this, register.getPort(Register.Ports.PORT_OUT), "Out", width, 2)
        )

        clockConnection = connections[2]

        init(register, properties, connections)
    }

    override fun keyPressed(
        manager: CircuitManager,
        state: CircuitState,
        keyCode: KeyCode,
        text: String
    ): Boolean {
        when (keyCode) {
            KeyCode.DIGIT0, KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4, KeyCode.DIGIT5, KeyCode.DIGIT6, KeyCode.DIGIT7, KeyCode.DIGIT8, KeyCode.DIGIT9, KeyCode.NUMPAD0, KeyCode.NUMPAD1, KeyCode.NUMPAD2, KeyCode.NUMPAD3, KeyCode.NUMPAD4, KeyCode.NUMPAD5, KeyCode.NUMPAD6, KeyCode.NUMPAD7, KeyCode.NUMPAD8, KeyCode.NUMPAD9, KeyCode.A, KeyCode.B, KeyCode.C, KeyCode.D, KeyCode.E, KeyCode.F -> {
                val c = text[0]
                val value = if (c >= '0' && c <= '9') c.code - '0'.code else c.uppercaseChar().code - 'A'.code + 10
                val currentValue = state.getLastPushed(component.getPort(Register.Ports.PORT_OUT))
                val typedValue = of(value.toLong(), min(4, currentValue.bitSize))
                if (typedValue.value != value) {
                    typedValue.setAllBits(WireValue.State.ZERO) // to prevent typing '9' on a 3-bit value, producing 1
                }
                if (currentValue.bitSize <= 4) {
                    currentValue.set(typedValue)
                } else {
                    for (i in currentValue.bitSize - 1 downTo 4) {
                        currentValue.setBit(i, currentValue.getBit(i - 4))
                    }

                    for (i in 0..3) {
                        currentValue.setBit(i, typedValue.getBit(i))
                    }
                }
                state.pushValue(component.getPort(Register.Ports.PORT_OUT), currentValue)
            }

            else -> {}
        }

        return false
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        val value = circuitState!!.getLastPushed(component.getPort(Register.Ports.PORT_OUT)).hexString

        val x = screenX
        val y = screenY
        val width = screenWidth
        val height = screenHeight

        graphics.fill = Color.BLACK
        graphics.font = getFont(13)
        var i = 0
        while (i * 4 < value.length) {
            val endIndex = min(i * 4 + 4, value.length)
            val toPrint = value.substring(4 * i, endIndex)
            val bounds = getBounds(graphics.font, toPrint, false)
            graphics.fillText(toPrint, x + width * 0.5 - bounds.width * 0.5, (y + 11 + 10 * i).toDouble())
            i++
        }

        graphics.fill = Color.GRAY
        graphics.font = getFont(10)
        graphics.fillText("D", x + 3.0, y + height * 0.5 + 6)
        graphics.fillText("Q", x + width - 10.0, y + height * 0.5 + 6)
        graphics.fillText("en", x + 3.0, y + height - 7.0)
        graphics.fillText("0", x + width - 13.0, y + height - 4.0)

        graphics.stroke = Color.BLACK
        drawClockInput(graphics, clockConnection, Properties.Direction.SOUTH)
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Memory", "Register"),
                Image(RegisterPeer::class.java.getResourceAsStream("/images/Register.png")),
                Properties(), true
            )
        }
    }
}
