package com.ra4king.circuitsim.gui.peers.wiring

import com.ra4king.circuitsim.gui.CircuitManager
import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.drawShape
import com.ra4king.circuitsim.gui.GuiUtils.drawValue
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.GuiUtils.setBitColor
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.properties.PropertyValidators
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.components.wiring.Pin
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonType
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import javafx.stage.Modality
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @author Roi Atalla
 */
class PinPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Pin>(x, y, 0, 0) {
    init {
        val value: Any? = props.getValueOrDefault(IS_INPUT, false)
        val isInput = if (value is String) value.toBoolean() else value as Boolean

        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(
            Properties.Property(
                Properties.LABEL_LOCATION,
                if (isInput) Properties.Direction.WEST else Properties.Direction.EAST
            )
        )
        properties.ensureProperty(Properties.DIRECTION)
        properties.ensureProperty(Properties.BITSIZE)
        properties.ensureProperty(IS_INPUT)
        properties.mergeIfExists(props)

        val pin = Pin(
            properties.getValue(Properties.LABEL),
            properties.getValue(Properties.BITSIZE),
            properties.getValue(IS_INPUT)
        )
        width = max(2, min(8, pin.bitSize))
        height = ((1 + (pin.bitSize - 1) / 8) * 1.5).roundToInt()

        val connections = ArrayList<PortConnection>()
        when (properties.getValue(Properties.DIRECTION)) {
            Properties.Direction.EAST -> connections.add(PortConnection(this, pin.getPort(0), width, height / 2))
            Properties.Direction.WEST -> connections.add(PortConnection(this, pin.getPort(0), 0, height / 2))
            Properties.Direction.NORTH -> connections.add(PortConnection(this, pin.getPort(0), width / 2, 0))
            Properties.Direction.SOUTH -> connections.add(PortConnection(this, pin.getPort(0), width / 2, height))
        }

        init(pin, properties, connections)
    }

    val isInput: Boolean
        get() = component.isInput

    override fun mousePressed(manager: CircuitManager, state: CircuitState, x: Double, y: Double) {
        var state = state
        if (!this.isInput) {
            return
        }

        if (state != manager.circuit.topLevelState) {
            val alert = Alert(AlertType.CONFIRMATION)
            alert.initOwner(manager.simulatorWindow.stage)
            alert.initModality(Modality.WINDOW_MODAL)
            alert.title = "Switch to top-level state?"
            alert.headerText = "Switch to top-level state?"
            alert.contentText = "Cannot modify state of a subcircuit. Switch to top-level state?"
            val buttonType = alert.showAndWait()
            if (buttonType.isPresent && buttonType.get() == ButtonType.OK) {
                state = manager.circuit.topLevelState
                manager.switchToCircuitState()
                manager.circuitBoard.currentState = state
            } else {
                return
            }
        }

        val pin = component

        val value = state.getLastPushed(pin.getPort(Pin.Ports.PORT))
        if (pin.bitSize == 1) {
            pin.setValue(state, WireValue(1, if (value.getBit(0) == WireValue.State.ONE) WireValue.State.ZERO else WireValue.State.ONE))
        } else {
            val bitWidth = screenWidth / min(8.0, pin.bitSize.toDouble())
            val bitHeight = screenHeight / ((pin.bitSize - 1) / 8 + 1.0)

            val bitCol = (x / bitWidth).toInt()
            val bitRow = (y / bitHeight).toInt()

            val bit = pin.bitSize - 1 - (bitCol + bitRow * 8)
            if (bit >= 0 && bit < pin.bitSize) {
                val newValue = WireValue(value)
                newValue.setBit(bit,
                    if (value.getBit(bit) == WireValue.State.ONE) WireValue.State.ZERO
                    else WireValue.State.ONE
                )
                pin.setValue(state, newValue)
            }
        }
    }

    override fun keyPressed(
        manager: CircuitManager,
        state: CircuitState,
        keyCode: KeyCode,
        text: String
    ): Boolean {
        if (!this.isInput) {
            return false
        }

        when (keyCode) {
            KeyCode.NUMPAD0, KeyCode.NUMPAD1, KeyCode.DIGIT0, KeyCode.DIGIT1 -> {
                val value = text[0].code - '0'.code
                val currentValue = WireValue(state.getLastPushed(component.getPort(Pin.Ports.PORT)))
                for (i in currentValue.bitSize - 1 downTo 1) {
                    currentValue.setBit(i, currentValue.getBit(i - 1))
                }
                currentValue.setBit(0, if (value == 1) WireValue.State.ONE else WireValue.State.ZERO)
                component.setValue(state, currentValue)
            }

            else -> {}
        }

        return false
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))

        graphics.font = getFont(16, true)
        val port = component.getPort(Pin.Ports.PORT)
        val value = if (this.isInput) circuitState!!.getLastPushed(port) else circuitState!!.getLastReceived(port)
        if (circuitState.isShortCircuited(port.link)) {
            graphics.fill = Color.RED
        } else {
            if (value.bitSize == 1) {
                setBitColor(graphics, value.getBit(0))
            } else {
                graphics.fill = Color.WHITE
            }
        }
        graphics.stroke = Color.BLACK

        if (this.isInput) {
            drawShape(this) { v: Double, v1: Double, v2: Double, v3: Double -> graphics.fillRect(v, v1, v2, v3) }
            drawShape(this) { v: Double, v1: Double, v2: Double, v3: Double -> graphics.strokeRect(v, v1, v2, v3) }
        } else {
            graphics.fillRoundRect(
                screenX.toDouble(),
                screenY.toDouble(),
                screenWidth.toDouble(),
                screenHeight.toDouble(),
                20.0,
                20.0
            )
            graphics.strokeRoundRect(
                screenX.toDouble(),
                screenY.toDouble(),
                screenWidth.toDouble(),
                screenHeight.toDouble(),
                20.0,
                20.0
            )
        }

        graphics.fill = if (port.link.bitSize > 1) Color.BLACK else Color.WHITE
        drawValue(graphics, value.toString(), screenX, screenY, screenWidth)
    }

    companion object {
        val IS_INPUT =
            Properties.Property("Is input?", PropertyValidators.YESNO_VALIDATOR, true)

        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Wiring", "Input Pin"),
                Image(PinPeer::class.java.getResourceAsStream("/images/InputPin.png")),
                Properties(Properties.Property(IS_INPUT, true)), true
            )

            manager.addComponent(
                Pair("Wiring", "Output Pin"),
                Image(PinPeer::class.java.getResourceAsStream("/images/OutputPin.png")),
                Properties(
                    Properties.Property(IS_INPUT, false),
                    Properties.Property(Properties.DIRECTION, Properties.Direction.WEST)
                ), true
            )
        }
    }
}
