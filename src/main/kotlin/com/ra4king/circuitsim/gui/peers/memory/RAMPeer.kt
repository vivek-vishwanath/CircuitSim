package com.ra4king.circuitsim.gui.peers.memory

import com.ra4king.circuitsim.gui.CircuitManager
import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.drawClockInput
import com.ra4king.circuitsim.gui.GuiUtils.getBounds
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.properties.PropertyFileValidator
import com.ra4king.circuitsim.gui.properties.PropertyListValidator
import com.ra4king.circuitsim.gui.properties.PropertyMemoryValidator
import com.ra4king.circuitsim.gui.properties.PropertyMemoryValidator.MemoryLine
import com.ra4king.circuitsim.gui.properties.PropertyValidators
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import com.ra4king.circuitsim.simulator.components.memory.ADDRESSABILITY
import com.ra4king.circuitsim.simulator.components.memory.ADDRESSABILITY_PROP_NAME
import com.ra4king.circuitsim.simulator.components.memory.Addressability
import com.ra4king.circuitsim.simulator.components.memory.RAM
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.MenuItem
import javafx.scene.image.Image
import javafx.scene.paint.Color
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Roi Atalla
 */
class RAMPeer(props: Properties, x: Int, y: Int) : ComponentPeer<RAM>(x, y, 9, 5) {
    private val clockConnection: PortConnection

    val isSeparateLoadStore: Boolean
        get() = component.isSeparateLoadStore

    private val isEditorOpen = AtomicBoolean(false)

    init {
        val properties = Properties()
        val srcFile = Properties.Property("Source Data", PropertyValidators.FILE_VALIDATOR, null)
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.BITSIZE)
        properties.ensureProperty(Properties.ADDRESS_BITS)
        properties.ensureProperty(SEPARATE_LOAD_STORE_PORTS)
        properties.ensureProperty(srcFile)
        properties.mergeIfExists(props)

        val addressBits = properties.getValue(Properties.ADDRESS_BITS)
        val dataBits = properties.getValue(Properties.BITSIZE)
        val separateLoadStore = properties.getValue(SEPARATE_LOAD_STORE_PORTS)
        val file: PropertyFileValidator.FileWrapper? = properties.getValue(srcFile)

        properties.ensureProperty(ADDRESSABILITY[dataBits - 1])
        if (props.containsProperty(ADDRESSABILITY[dataBits - 1])) {
            val value: Addressability? = try {
                props.getValue(ADDRESSABILITY_PROP_NAME)
            } catch (_: ClassCastException) {
                try { Addressability.valueOf(props.getValue(ADDRESSABILITY_PROP_NAME)) }
                catch (_: IllegalArgumentException) { Addressability.WORD }
            }
            if (value in (ADDRESSABILITY[dataBits - 1].validator as PropertyListValidator).validValues)
                properties.setValue(ADDRESSABILITY[dataBits - 1], value)
        }
        val addressability = properties.getValue(ADDRESSABILITY[dataBits - 1])

        val ram = RAM(properties.getValue(Properties.LABEL), addressBits, dataBits, addressability, separateLoadStore, file?.file)

        val connections = arrayListOf(
            PortConnection(this, ram.getPort(RAM.Ports.PORT_ADDRESS), "Address", 0, 2), 
            PortConnection(this, ram.getPort(RAM.Ports.PORT_CLK), "Clock", 3, height)
        )
        clockConnection = connections[1]
        connections.add(PortConnection(this, ram.getPort(RAM.Ports.PORT_ENABLE), "Enable", 4, height))
        connections.add(PortConnection(this, ram.getPort(RAM.Ports.PORT_LOAD), "Load", 5, height))
        connections.add(PortConnection(this, ram.getPort(RAM.Ports.PORT_DATA), "Data", width, 2))
        if (separateLoadStore) {
            connections.add(PortConnection(this, ram.getPort(RAM.Ports.PORT_DATA_IN), "Data Input", 0, 4))
            connections.add(PortConnection(this, ram.getPort(RAM.Ports.PORT_STORE), "Store", 6, height))
            connections.add(PortConnection(this, ram.getPort(RAM.Ports.PORT_CLEAR), "Clear", 7, height))
        } else {
            connections.add(PortConnection(this, ram.getPort(RAM.Ports.PORT_CLEAR), "Clear", 6, height))
        }

        init(ram, properties, connections)
    }

    override fun getContextMenuItems(circuit: CircuitManager): MutableList<MenuItem> {
        val menuItem = MenuItem("Edit contents")
        menuItem.setOnAction {
            val ram = component
            val memoryValidator =
                PropertyMemoryValidator(ram)

            val memory = ArrayList<MemoryLine>()
            val listener = { address: Int, data: Int ->
                val index = address / 16
                val line = memory[index]
                line.values[address - index * 16].value = memoryValidator.formatValue(data)
            }

            if (isEditorOpen.getAndSet(true)) {
                return@setOnAction
            }
            try {
                // Internal state can change in between and data can get out of sync
                val simulatorWindow = circuit.simulatorWindow
                simulatorWindow.simulator.runSync {
                    ram.addMemoryListener(listener)
                    val currentState = circuit.circuitBoard.currentState
                    memory.addAll(
                        memoryValidator.parseLine(ram.getMemoryContents(currentState))
                        { address: Int, value: Int ->
                            simulatorWindow.simulator.runSync {
                                // Component has been removed
                                if (ram.circuit == null) {
                                    return@runSync
                                }
                                ram.store(currentState, ram.effective(address), value)
                            }
                        }
                    )
                }

                memoryValidator.createAndShowMemoryWindow(simulatorWindow.stage, memory)
            } finally {
                ram.removeMemoryListener(listener)
                isEditorOpen.set(false)
            }
        }
        return mutableListOf(menuItem)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        graphics.stroke = Color.BLACK
        drawClockInput(graphics, clockConnection, Properties.Direction.SOUTH)

        val addressVal = circuitState!!.getLastReceived(component.getPort(RAM.Ports.PORT_ADDRESS))
        val valueVal: WireValue?
        if (addressVal.isValidValue) {
            val `val` = component.load(circuitState, addressVal.value)
            valueVal = of(`val`.toLong(), component.dataBits)
        } else {
            valueVal = WireValue(component.dataBits)
        }

        val address = addressVal.hexString
        val value = valueVal.hexString

        val x = screenX
        val y = screenY
        val width = screenWidth
        val height = screenHeight

        graphics.font = getFont(11, true)

        var text = "RAM"
        var bounds = getBounds(graphics.font, text)
        graphics.fill = Color.BLACK
        graphics.fillText(text, x + (width - bounds.width) * 0.5, y + (height + bounds.height) * 0.2)


        // Draw address
        text = "A: $address"
        val addrY = y + bounds.height + 12
        graphics.fillText(text, x + 13.0, addrY)


        // Draw data afterward
        bounds = getBounds(graphics.font, text)
        graphics.fillText("D: $value", x + 13.0, addrY + bounds.height)

        graphics.fill = Color.GRAY
        graphics.font = getFont(10)
        graphics.fillText("A", x + 3.0, y + height * 0.5 - 1)
        graphics.fillText("D", x + width - 9.0, y + height * 0.5 - 1)
        graphics.fillText("en", x + width * 0.5 - 11.5, y + height - 3.5)
        graphics.fillText("L", x + width * 0.5 + 2, y + height - 3.5)

        if (this.isSeparateLoadStore) {
            graphics.fillText("Din", x + 3.0, y + height * 0.5 + 20)
            graphics.fillText("St", x + width * 0.5 + 8.5, y + height - 3.5)
            graphics.fillText("0", x + width * 0.5 + 21.5, y + height - 3.5)
        } else {
            graphics.fillText("0", x + width * 0.5 + 11.5, y + height - 3.5)
        }
    }

    companion object {
        val SEPARATE_LOAD_STORE_PORTS =
            Properties.Property("Separate Load/Store Ports?", PropertyValidators.YESNO_VALIDATOR, false)

        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Memory", "RAM"),
                Image(RAMPeer::class.java.getResourceAsStream("/images/RAM.png")),
                Properties(SEPARATE_LOAD_STORE_PORTS), true
            )
        }
    }
}
