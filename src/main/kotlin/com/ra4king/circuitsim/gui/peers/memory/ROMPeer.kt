package com.ra4king.circuitsim.gui.peers.memory

import com.ra4king.circuitsim.gui.CircuitManager
import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.EditHistory
import com.ra4king.circuitsim.gui.GuiUtils.getBounds
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.properties.PropertyListValidator
import com.ra4king.circuitsim.gui.properties.PropertyMemoryValidator
import com.ra4king.circuitsim.gui.properties.PropertyMemoryValidator.MemoryLine
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.components.memory.ADDRESSABILITY
import com.ra4king.circuitsim.simulator.components.memory.ADDRESSABILITY_PROP_NAME
import com.ra4king.circuitsim.simulator.components.memory.Addressability
import com.ra4king.circuitsim.simulator.components.memory.ROM
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.MenuItem
import javafx.scene.image.Image
import javafx.scene.paint.Color
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.flatMap
import kotlin.collections.toIntArray
import kotlin.text.toUInt

/**
 * @author Roi Atalla
 */
class ROMPeer(props: Properties, x: Int, y: Int) : ComponentPeer<ROM>(x, y, 9, 5) {
    private val contentsProperty: Properties.Property<MutableList<MemoryLine>>

    private val isEditorOpen = AtomicBoolean(false)

    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.BITSIZE)
        properties.ensureProperty(Properties.ADDRESS_BITS)
        properties.mergeIfExists(props)

        val addressBits = properties.getValue(Properties.ADDRESS_BITS)
        val dataBits = properties.getValue(Properties.BITSIZE)

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

        val rom = ROM(properties.getValue(Properties.LABEL), addressBits, dataBits, addressability)

        contentsProperty = Properties.Property(
            "Contents",
            PropertyMemoryValidator(rom),
            null
        )
        val oldMemory: String?
        val oldContents = props.getProperty<Any?>("Contents")
        oldMemory = when {
            oldContents == null -> ""
            oldContents.validator == null -> props.getValue("Contents")
            else -> oldContents.stringValue
        }
        properties.setValue(contentsProperty, contentsProperty.validator.parse(oldMemory))

        val memory: IntArray = properties.getValue(contentsProperty)?.flatMap { it.values }
            ?.map { it.get().toUInt(16).toInt() }?.toIntArray() ?: IntArray(0)
        rom.initMemory(memory)

        val connections = arrayListOf(
            PortConnection(this, rom.getPort(ROM.Ports.PORT_ADDRESS), "Address", 0, 2),
            PortConnection(this, rom.getPort(ROM.Ports.PORT_ENABLE), "Enable", 4, height),
            PortConnection(this, rom.getPort(ROM.Ports.PORT_DATA), "Data", width, 2)
        )
        
        init(rom, properties, connections)
    }

    override fun getContextMenuItems(circuit: CircuitManager): MutableList<MenuItem> {
        val menuItem = MenuItem("Edit contents")
        menuItem.setOnAction {
            val rom = component
            val property: Properties.Property<MutableList<MemoryLine>?> =
                properties.getProperty(contentsProperty.name)
            val memoryValidator = property.validator as PropertyMemoryValidator

            val lines = ArrayList<MemoryLine>()
            val listener = { address: Int, data: Int ->
                val index = address / 16
                val line = property.value!![index]
                line.values[address - index * 16].value = memoryValidator.formatValue(data)
                circuit.circuit.forEachState { state: CircuitState? ->
                    rom.valueChanged(
                        state!!, WireValue(
                            WireValue.State.Z
                        ), 0
                    )
                }
            }

            if (isEditorOpen.getAndSet(true)) {
                return@setOnAction
            }
            try {
                val simulatorWindow = circuit.simulatorWindow
                simulatorWindow.simulator.runSync {
                    lines.addAll(memoryValidator.parseLine(rom.memory) { address: Int, newValue: Int ->
                        simulatorWindow.simulator.runSync {
                            // Component has been removed
                            if (rom.circuit == null) {
                                return@runSync
                            }

                            val oldValue = rom.load(address)
                            rom.store(address, newValue)
                            simulatorWindow.editHistory.addAction(object : EditHistory.Edit(circuit) {
                                override fun undo() {
                                    rom.store(address, oldValue)
                                }

                                override fun redo() {
                                    rom.store(address, newValue)
                                }
                            })
                        }
                    })
                    rom.addMemoryListener(listener)
                }

                memoryValidator.createAndShowMemoryWindow(simulatorWindow.stage, lines)
            } finally {
                rom.removeMemoryListener(listener)
                isEditorOpen.set(false)
            }
        }
        return mutableListOf(menuItem)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        val address = circuitState!!.getLastReceived(component.getPort(ROM.Ports.PORT_ADDRESS)).hexString
        val value = circuitState.getLastPushed(component.getPort(ROM.Ports.PORT_DATA)).hexString

        val x = screenX
        val y = screenY
        val width = screenWidth
        val height = screenHeight

        graphics.font = getFont(11, true)

        var text = "ROM"
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
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Memory", "ROM"),
                Image(ROMPeer::class.java.getResourceAsStream("/images/ROM.png")),
                Properties(), true
            )
        }
    }
}
