package com.ra4king.circuitsim.gui.peers.wiring

import com.ra4king.circuitsim.gui.CircuitSimVersion
import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.drawValue
import com.ra4king.circuitsim.gui.GuiUtils.drawValueOneLine
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.GuiUtils.setBitColor
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.properties.IntegerString
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import com.ra4king.circuitsim.simulator.components.wiring.Constant
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @author Roi Atalla
 */
class ConstantPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Constant>(x, y, 0, 0) {
    private val value: WireValue

    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.DIRECTION)
        properties.ensureProperty(Properties.BITSIZE)
        properties.ensureProperty(Properties.BASE)


        // VALUE property:
        // This property depends on BASE. So we have to merge BASE first, so the value is updated
        // before we can create VALUE (we actually merge every property except VALUE, but that's fine).
        //
        // Then, we create VALUE. Finally, we update it with the old value (if one exists).
        // This is also special for VALUE, because for IntegerString values, 
        // it should update to reflect the new base.
        // So, if the old value was an IntegerString, we override the default.
        val oldValueProperty = props.getProperty<Any>(Properties.VALUE.name)
        properties.clearProperty(Properties.VALUE.name)
        properties.mergeIfExists(props)

        val base = properties.getValue(Properties.BASE)
        val valueProperty = Properties.VALUE(base.value)
        properties.ensureProperty(valueProperty)

        // Handling previous input:
        if (oldValueProperty != null) {
            if (oldValueProperty.value is IntegerString) {
                val valStr = oldValueProperty.value
                if (valStr.base != base.value) {
                    // Replace with value in new base:
                    val newText = valStr.toString(base.value)
                    properties.parseAndSetValue(valueProperty, newText)
                } else {
                    // If base is the same, repropagate the old value (but not the validator)
                    properties.setValue(valueProperty, valStr)
                }
            } else if (props.version <= MAXIMUM_VERSION_FOR_LEGACY_VALUE_PARSING) {
                // On importing a legacy file:
                // Convert all legacy constants (base 10) by converting string to display base:
                val valStr = IntegerString.parseFromLegacy(oldValueProperty.stringValue, base.value)
                properties.setValue(valueProperty, valStr)
            } else {
                // On importing a regular file:
                properties.updateIfExists(oldValueProperty)
            }
        }

        //
        val constant = Constant(
            properties.getValue(Properties.LABEL),
            properties.getValue(Properties.BITSIZE),
            properties.getValue(valueProperty).value
        )

        val bitSize = constant.bitSize
        when (base) {
            Properties.Base.BINARY -> {
                width = max(2, min(8, bitSize))
                height = ((1 + (bitSize - 1) / 8) * 1.5).roundToInt()
            }

            Properties.Base.HEXADECIMAL -> {
                width = max(2, 1 + (bitSize - 1) / 4)
                height = 2
            }

            Properties.Base.DECIMAL -> {
                // 3.322 ~ log_2(10)
                var width = max(2, ceil(bitSize / 3.322).toInt())
                width += if (bitSize == 32) 1 else 0
                this.width = width
                height = 2
            }
        }

        value = of(constant.value.toLong(), bitSize)

        val connections = ArrayList<PortConnection>()
        when (properties.getValue(Properties.DIRECTION)) {
            Properties.Direction.EAST -> connections.add(PortConnection(this, constant.getPort(0), width, height / 2))
            Properties.Direction.WEST -> connections.add(PortConnection(this, constant.getPort(0), 0, height / 2))
            Properties.Direction.NORTH -> connections.add(PortConnection(this, constant.getPort(0), width / 2, 0))
            Properties.Direction.SOUTH -> connections.add(PortConnection(this, constant.getPort(0), width / 2, height))
        }

        init(constant, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))

        graphics.font = getFont(16)
        graphics.fill = Color.GRAY
        graphics.stroke = Color.GRAY

        graphics.fillRoundRect(
            screenX.toDouble(),
            screenY.toDouble(),
            screenWidth.toDouble(),
            screenHeight.toDouble(),
            10.0,
            10.0
        )
        graphics.strokeRoundRect(
            screenX.toDouble(),
            screenY.toDouble(),
            screenWidth.toDouble(),
            screenHeight.toDouble(),
            10.0,
            10.0
        )

        val valStr = when (properties.getValue(Properties.BASE)) {
            Properties.Base.BINARY -> value.toString()
            Properties.Base.HEXADECIMAL -> value.hexString
            Properties.Base.DECIMAL -> value.decString
        }

        if (value.bitSize > 1) {
            graphics.fill = Color.BLACK
        } else {
            setBitColor(graphics, value.getBit(0))
        }

        if (properties.getValue(Properties.BASE) == Properties.Base.DECIMAL) {
            drawValueOneLine(graphics, valStr, screenX, screenY, screenWidth)
        } else {
            drawValue(graphics, valStr, screenX, screenY, screenWidth)
        }
    }

    companion object {
        /**
         * In 1.9.2b and prior, unprefixed constants resolve to base 10.
         * In 1.10.0 [2110 edition] and after, unprefixed constants resolve to the display base.
         */
        private val MAXIMUM_VERSION_FOR_LEGACY_VALUE_PARSING = CircuitSimVersion("1.9.2b")

        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Wiring", "Constant"),
                Image(ConstantPeer::class.java.getResourceAsStream("/images/Constant.png")),
                Properties(), true
            )
        }
    }
}
