package com.ra4king.circuitsim.gui.peers.gates

import com.ra4king.circuitsim.gui.CircuitSimVersion
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.rotateElementSize
import com.ra4king.circuitsim.gui.GuiUtils.rotateGraphics
import com.ra4king.circuitsim.gui.GuiUtils.setBitColor
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.properties.PropertyValidators
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.gates.Gate
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import kotlin.math.max
import kotlin.math.min

/**
 * @author Roi Atalla
 */
abstract class GatePeer<T : Gate> @JvmOverloads constructor(
    props: Properties,
    x: Int,
    y: Int,
    width: Int = 4,
    height: Int = 4,
    allowNegatingInputs: Boolean = true
) : ComponentPeer<T>(x, y, width, height) {
    private var hasExpandedInputs = false

    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.DIRECTION)
        ensureProperties(properties)
        properties.mergeIfExists(props)

        var negationCounts = 0
        if (allowNegatingInputs) {
            while (true) {
                val propName = "Negate " + negationCounts++
                val property = Properties.Property(propName, PropertyValidators.YESNO_VALIDATOR, false)
                if (props.containsProperty(propName)) {
                    props.ensureProperty(property)
                    properties.setProperty(Properties.Property(property, props.getValue(property)))
                } else {
                    break
                }
            }
        }

        val gate = buildGate(properties)
        val gateNum = gate!!.numInputs

        var hasNegatedInput = false
        if (allowNegatingInputs) {
            for (i in 0..<max(gateNum, negationCounts)) {
                val propName = "Negate $i"

                if (i < gateNum) {
                    val property = Properties.Property(propName, PropertyValidators.YESNO_VALIDATOR, false)
                    if (i == 0) {
                        property.display += " Top/Left"
                    } else if (i == gateNum - 1) {
                        property.display += " Bottom/Right"
                    }

                    val negate = props.getValueOrDefault(property, false)
                    properties.setProperty(Properties.Property(property, negate))

                    hasNegatedInput = hasNegatedInput or negate
                } else {
                    properties.clearProperty(propName)
                }
            }
        }

        props.ensurePropertyIfExists(LEGACY_GATE_INPUT_PLACEMENT)

        val forceLegacyInputPlacement =
            props.version <= MAXIMUM_VERSION_FOR_LEGACY_GATE_INPUT_PLACEMENT ||
                    props.getValueOrDefault(LEGACY_GATE_INPUT_PLACEMENT, false)


        // Expand the width (in the default configuration) by 1 if any inputs are negated or if there are more than
        // five inputs. This will show the additional line in for each gate to avoid confusing "floating" ports.
        if (hasNegatedInput || (gateNum > 5 && !forceLegacyInputPlacement)) {
            hasExpandedInputs = true
            this.width = width + 1
        } else if (gateNum > 5) {
            // Legacy behavior where the file was saved with an older version
            properties.setValue(LEGACY_GATE_INPUT_PLACEMENT, true)
        }

        rotateElementSize(
            this,
            Properties.Direction.EAST,
            properties.getValue(Properties.DIRECTION)
        )

        val connections = ArrayList<PortConnection>()

        var inputOffset = 0
        when (properties.getValue(Properties.DIRECTION)) {
            Properties.Direction.WEST -> {
                inputOffset = this.width
                var i = 0
                while (i < gateNum) {
                    val add = if (gateNum % 2 == 0 && i >= gateNum / 2) 3 else 2
                    connections.add(
                        PortConnection(
                            this,
                            gate.getPort(i),
                            inputOffset,
                            i + add - gateNum / 2 - (if (gateNum == 1) 1 else 0)
                        )
                    )
                    i++
                }

                connections.add(
                    PortConnection(
                        this,
                        gate.getPort(gateNum),
                        0,
                        height / 2
                    )
                )
            }

            Properties.Direction.EAST -> {
                var i = 0
                while (i < gateNum) {
                    val add = if (gateNum % 2 == 0 && i >= gateNum / 2) 3 else 2
                    connections.add(
                        PortConnection(
                            this,
                            gate.getPort(i),
                            inputOffset,
                            i + add - gateNum / 2 - (if (gateNum == 1) 1 else 0)
                        )
                    )
                    i++
                }

                connections.add(
                    PortConnection(
                        this,
                        gate.getPort(gateNum),
                        this.width,
                        height / 2
                    )
                )
            }

            Properties.Direction.NORTH -> {
                inputOffset = height
                var i = 0
                while (i < gateNum) {
                    val add = if (gateNum % 2 == 0 && i >= gateNum / 2) 3 else 2
                    connections.add(
                        PortConnection(
                            this,
                            gate.getPort(i),
                            i + add - gateNum / 2 - (if (gateNum == 1) 1 else 0),
                            inputOffset
                        )
                    )
                    i++
                }

                connections.add(
                    PortConnection(
                        this,
                        gate.getPort(gateNum),
                        this.width / 2,
                        0
                    )
                )
            }

            Properties.Direction.SOUTH -> {
                var i = 0
                while (i < gateNum) {
                    val add = if (gateNum % 2 == 0 && i >= gateNum / 2) 3 else 2
                    connections.add(
                        PortConnection(
                            this,
                            gate.getPort(i),
                            i + add - gateNum / 2 - (if (gateNum == 1) 1 else 0),
                            inputOffset
                        )
                    )
                    i++
                }

                connections.add(
                    PortConnection(
                        this,
                        gate.getPort(gateNum),
                        this.width / 2,
                        height
                    )
                )
            }
        }

        init(gate, properties, connections)
    }

    protected abstract fun ensureProperties(properties: Properties)

    abstract fun buildGate(properties: Properties): T?

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        var minPortX = 0
        var minPortY = 0
        var maxPortX = 0
        var maxPortY = 0
        val direction = properties.getValue(Properties.DIRECTION)

        for (i in 0..<connections.size - 1) {
            val portConnection = connections[i]
            var x = portConnection.x * GuiUtils.BLOCK_SIZE
            var y = portConnection.y * GuiUtils.BLOCK_SIZE

            if (i == 0) {
                maxPortX = x
                minPortX = maxPortX
                maxPortY = y
                minPortY = maxPortY
            } else {
                minPortX = min(minPortX, x)
                minPortY = min(minPortY, y)
                maxPortX = max(maxPortX, x)
                maxPortY = max(maxPortY, y)
            }

            if (component.negateInputs[i]) {
                graphics.fill = Color.WHITE
                graphics.stroke = Color.BLACK
                graphics.lineWidth = 1.0

                when (direction) {
                    Properties.Direction.WEST -> {
                        x -= GuiUtils.BLOCK_SIZE
                        y = (y - GuiUtils.BLOCK_SIZE * 0.5).toInt()
                    }

                    Properties.Direction.EAST -> y = (y - GuiUtils.BLOCK_SIZE * 0.5).toInt()
                    Properties.Direction.NORTH -> {
                        y -= GuiUtils.BLOCK_SIZE
                        x = (x - GuiUtils.BLOCK_SIZE * 0.5).toInt()
                    }

                    Properties.Direction.SOUTH -> x = (x - GuiUtils.BLOCK_SIZE * 0.5).toInt()
                }

                graphics.fillOval(
                    x.toDouble(),
                    y.toDouble(),
                    GuiUtils.BLOCK_SIZE.toDouble(),
                    GuiUtils.BLOCK_SIZE.toDouble()
                )
                graphics.strokeOval(
                    x.toDouble(),
                    y.toDouble(),
                    GuiUtils.BLOCK_SIZE.toDouble(),
                    GuiUtils.BLOCK_SIZE.toDouble()
                )
            } else if (hasExpandedInputs) {
                // Imitate how a wire is drawn in Wire.paint()
                setBitColor(graphics, circuitState!!.getLastReceived(portConnection.port))
                graphics.lineWidth = 2.0

                val dx = when (direction) {
                    Properties.Direction.WEST -> -(GuiUtils.BLOCK_SIZE - 1)
                    Properties.Direction.EAST -> GuiUtils.BLOCK_SIZE - 1
                    else -> 0
                }
                val dy = when (direction) {
                    Properties.Direction.NORTH -> -(GuiUtils.BLOCK_SIZE - 1)
                    Properties.Direction.SOUTH -> GuiUtils.BLOCK_SIZE - 1
                    else -> 0
                }

                graphics.strokeLine(x.toDouble(), y.toDouble(), (x + dx).toDouble(), (y + dy).toDouble())
            }
        }


        // Reset the line width to the default after possibly drawing some fake wires drawn above
        graphics.lineWidth = 1.0
        graphics.stroke = Color.BLACK


        // Draw a thin black line to reach ports that are beyond the width/height of the gate.
        // This prevents fake wires or ports from hanging off the edge of the gate, which can
        // confuse students.
        var pad: Int = if (hasExpandedInputs)
            // Need to account for the bubbles drawn above
            when (direction) {
                Properties.Direction.NORTH, Properties.Direction.WEST -> -GuiUtils.BLOCK_SIZE
                Properties.Direction.EAST, Properties.Direction.SOUTH -> GuiUtils.BLOCK_SIZE
            }
        else 0

        pad += when (direction) {
            Properties.Direction.WEST -> screenWidth
            Properties.Direction.NORTH -> screenHeight
            else -> 0
        }

        val screenX = this.screenX.toDouble()
        val screenY = this.screenY.toDouble()
        when (direction) {
            Properties.Direction.WEST, Properties.Direction.EAST -> {
                if (minPortY < screenY) {
                    // The -1s and +1s here are to account for the width of fake wires drawn above
                    // (otherwise they overhang this little black line)
                    graphics.strokeLine(
                        screenX + pad,
                        minPortY - 1.0,
                        screenX + pad,
                        screenY
                    )
                }
                if (maxPortY > screenY + screenHeight) {
                    graphics.strokeLine(
                        screenX + pad,
                        maxPortY + 1.0,
                        screenX + pad,
                        screenY + screenHeight
                    )
                }
            }

            Properties.Direction.NORTH, Properties.Direction.SOUTH -> {
                if (minPortX < screenX) {
                    graphics.strokeLine(
                        minPortX - 1.0,
                        screenY + pad,
                        screenX,
                        screenY + pad
                    )
                }
                if (maxPortX > screenX + screenWidth) {
                    graphics.strokeLine(
                        maxPortX + 1.0,
                        screenY + pad,
                        screenX + screenWidth,
                        screenY + pad
                    )
                }
            }
        }

        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))
        rotateGraphics(this, graphics, direction)

        if (hasExpandedInputs) {
            graphics.translate(GuiUtils.BLOCK_SIZE.toDouble(), 0.0)
        }

        paintGate(graphics, circuitState)
    }

    abstract fun paintGate(graphics: GraphicsContext, circuitState: CircuitState?)

    companion object {
        private val MAXIMUM_VERSION_FOR_LEGACY_GATE_INPUT_PLACEMENT = CircuitSimVersion("1.8.5")
        private val LEGACY_GATE_INPUT_PLACEMENT = Properties.Property(
            "Legacy Gate Input Placement",
            "Legacy Gate Input Placement",
            "If enabled, no offset is " + "used for gates with more " + "than 5 inputs.",
            PropertyValidators.YESNO_VALIDATOR,
            false
        )

        @JvmStatic
        protected fun parseNegatedInputs(inputs: Int, properties: Properties): BooleanArray {
            val negated = BooleanArray(inputs)

            for (i in negated.indices) {
                negated[i] = properties.getValueOrDefault("Negate $i", false)
            }

            return negated
        }
    }
}
