package com.ra4king.circuitsim.gui.peers.plexers

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.rotateElementSize
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.plexers.PriorityEncoder
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color

/**
 * @author Elliott Childre
 */
class PriorityEncoderPeer(props: Properties, x: Int, y: Int) :
    ComponentPeer<PriorityEncoder>(x, y, ENABLED_INOUT_SIDE_LEN.toInt(), 0) {
    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.DIRECTION)
        properties.ensureProperty(Properties.SELECTOR_BITS)
        properties.setValue(Properties.SELECTOR_BITS, 3)
        properties.mergeIfExists(props)

        val pEncoder = PriorityEncoder(
            properties.getValue(Properties.LABEL),
            properties.getValue(Properties.SELECTOR_BITS)
        )
        val numInputs = 1 shl pEncoder.numSelectBits
        val inputSideLen = numInputs + 1
        height = inputSideLen

        rotateElementSize(
            this,
            Properties.Direction.EAST,
            properties.getValue(Properties.DIRECTION)
        )

        val connections = ArrayList<PortConnection>(numInputs + 4)
        var i: Int
        when (properties.getValue(Properties.DIRECTION)) {
            Properties.Direction.EAST -> {
                i = 0
                while (i < numInputs) {
                    connections.add(PortConnection(this, pEncoder.getPort(i), i.toString(), 0, i + 1))
                    i++
                }
                connections.add(PortConnection(this,pEncoder.enabledInPort,"Enable In",ENABLED_INOUT_SIDE_LEN.toInt() shr 1,inputSideLen))
                connections.add(PortConnection(this,pEncoder.enabledOutPort,"Enable Out",ENABLED_INOUT_SIDE_LEN.toInt() shr 1,0))
                connections.add(PortConnection(this,pEncoder.groupSignalPort,"Group Signal",ENABLED_INOUT_SIDE_LEN.toInt(),(inputSideLen shr 1) + 1))
                connections.add(PortConnection(this,pEncoder.outputPort,"Output",ENABLED_INOUT_SIDE_LEN.toInt(),inputSideLen shr 1))
            }

            Properties.Direction.WEST -> {
                i = 0
                while (i < numInputs) {
                    connections.add(PortConnection(this,pEncoder.getPort(i),i.toString(),ENABLED_INOUT_SIDE_LEN.toInt(),i + 1))
                    i++
                }
                connections.add(PortConnection(this,pEncoder.enabledInPort,"Enable In",ENABLED_INOUT_SIDE_LEN.toInt() shr 1,inputSideLen))
                connections.add(PortConnection(this,pEncoder.enabledOutPort,"Enable Out",ENABLED_INOUT_SIDE_LEN.toInt() shr 1,0))
                connections.add(PortConnection(this,pEncoder.groupSignalPort,"Group Signal",0,(inputSideLen shr 1) + 1))
                connections.add(PortConnection(this, pEncoder.outputPort, "Output", 0, inputSideLen shr 1))
            }

            Properties.Direction.SOUTH -> {
                i = 0
                while (i < numInputs) {
                    connections.add(PortConnection(this, pEncoder.getPort(i), i.toString(), i + 1, 0))
                    i++
                }
                connections.add(PortConnection(this,pEncoder.enabledInPort,"Enable In",0,ENABLED_INOUT_SIDE_LEN.toInt() shr 1))
                connections.add(PortConnection(this,pEncoder.enabledOutPort,"Enable Out",inputSideLen,ENABLED_INOUT_SIDE_LEN.toInt() shr 1))
                connections.add(PortConnection(this,pEncoder.groupSignalPort,"Group Signal",inputSideLen shr 1,ENABLED_INOUT_SIDE_LEN.toInt()))
                connections.add(PortConnection(this,pEncoder.outputPort,"Output",(inputSideLen shr 1) + 1,ENABLED_INOUT_SIDE_LEN.toInt()))
            }

            Properties.Direction.NORTH -> {
                i = 0
                while (i < numInputs) {
                    connections.add(PortConnection(this,pEncoder.getPort(i),i.toString(),i + 1,ENABLED_INOUT_SIDE_LEN.toInt()))
                    i++
                }
                connections.add(PortConnection(this,pEncoder.enabledInPort,"Enable In",inputSideLen,ENABLED_INOUT_SIDE_LEN.toInt() shr 1))
                connections.add(PortConnection(this,pEncoder.enabledOutPort,"Enable Out",0,ENABLED_INOUT_SIDE_LEN.toInt() shr 1))
                connections.add(PortConnection(this,pEncoder.groupSignalPort,"Group Signal",(inputSideLen shr 1) + 1,0))
                connections.add(PortConnection(this, pEncoder.outputPort, "Output", inputSideLen shr 1, 0))
            }

            else -> throw RuntimeException("Unknown Direction")
        }
        init(pEncoder, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))

        val direction = properties.getValue(Properties.DIRECTION)
        graphics.translate(screenX.toDouble(), screenY.toDouble())

        val height: Int
        val width: Int

        val inputSideLength = ((1 shl component.numSelectBits) + 1) * GuiUtils.BLOCK_SIZE
        val enabledSideLength: Int = ENABLED_INOUT_SIDE_LEN * GuiUtils.BLOCK_SIZE

        var zeroX = (GuiUtils.BLOCK_SIZE shr 1).toDouble()
        var zeroY: Double

        if (direction == Properties.Direction.EAST || direction == Properties.Direction.WEST) {
            height = inputSideLength
            width = enabledSideLength
            zeroY = GuiUtils.BLOCK_SIZE * 1.5
            if (direction == Properties.Direction.WEST) {
                zeroX = (width - GuiUtils.BLOCK_SIZE).toDouble()
            }
        } else {
            height = enabledSideLength
            width = inputSideLength
            zeroY = (height - (GuiUtils.BLOCK_SIZE shr 1)).toDouble()
            if (direction == Properties.Direction.SOUTH) {
                zeroY = GuiUtils.BLOCK_SIZE * 1.5
            }
        }

        graphics.stroke = Color.BLACK
        graphics.strokeRect(0.0, 0.0, width.toDouble(), height.toDouble())

        graphics.fill = Color.WHITE
        graphics.fillRect(0.0, 0.0, width.toDouble(), height.toDouble())

        graphics.fill = Color.DARKGRAY
        graphics.fillText("0", zeroX, zeroY)

        graphics.fill = Color.BLACK
        graphics.fillText(
            "Pri",
            (width shr 1) - graphics.font.size,
            (height shr 1) + 0.5 * GuiUtils.BLOCK_SIZE
        )
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Plexer", "Priority Encoder"),
                Image(PriorityEncoderPeer::class.java.getResourceAsStream("/images/PriorityEncoder.png")),
                Properties(), true
            )
        }

        private const val ENABLED_INOUT_SIDE_LEN: Byte = 4
    }
}
