package com.ra4king.circuitsim.gui.peers.plexers

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.rotateElementSize
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.plexers.Demultiplexer
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import kotlin.math.min

/**
 * @author Roi Atalla
 */
class DemultiplexerPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Demultiplexer>(x, y, 3, 0) {
    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.DIRECTION)
        properties.ensureProperty(Properties.SELECTOR_LOCATION)
        properties.ensureProperty(Properties.BITSIZE)
        properties.ensureProperty(Properties.SELECTOR_BITS)
        properties.mergeIfExists(props)

        val demux = Demultiplexer(
            properties.getValue(Properties.LABEL),
            properties.getValue(Properties.BITSIZE),
            properties.getValue(Properties.SELECTOR_BITS)
        )
        height = demux.numOutputs + 2

        rotateElementSize(
            this,
            Properties.Direction.EAST,
            properties.getValue(Properties.DIRECTION)
        )

        val location = properties.getValue(Properties.SELECTOR_LOCATION)

        var outOffset = 0
        var selOffset: Int
        val connections = ArrayList<PortConnection>()
        when (properties.getValue(Properties.DIRECTION)) {
            Properties.Direction.EAST -> {
                outOffset = width
                selOffset = 1
                var i = 0
                while (i < demux.numOutputs) {
                    connections.add(PortConnection(this, demux.getPort(i), i.toString(), outOffset, i + 1))
                    i++
                }
                connections.add(PortConnection(this,demux.selectorPort,"Selector",width / 2 + selOffset,if (location) 0 else height))
                connections.add(PortConnection(this,demux.inputPort,"In",width - outOffset,height / 2))
            }

            Properties.Direction.WEST -> {
                var i = 0
                while (i < demux.numOutputs) {
                    connections.add(PortConnection(this, demux.getPort(i), i.toString(), outOffset, i + 1))
                    i++
                }
                connections.add(PortConnection(this,demux.selectorPort,"Selector",width / 2,if (location) 0 else height))
                connections.add(PortConnection(this,demux.inputPort,"In",width,height / 2))
            }

            Properties.Direction.SOUTH -> {
                outOffset = height
                selOffset = 1
                var i = 0
                while (i < demux.numOutputs) {
                    connections.add(PortConnection(this, demux.getPort(i), i.toString(), i + 1, outOffset))
                    i++
                }
                connections.add(PortConnection(this,demux.selectorPort,"Selector",if (location) 0 else width,height / 2 + selOffset))
                connections.add(PortConnection(this,demux.inputPort,"In",width / 2,height - outOffset))
            }

            Properties.Direction.NORTH -> {
                var i = 0
                while (i < demux.numOutputs) {
                    connections.add(PortConnection(this, demux.getPort(i), i.toString(), i + 1, outOffset))
                    i++
                }
                connections.add(PortConnection(this,demux.selectorPort,"Selector",if (location) 0 else width,height / 2))
                connections.add(PortConnection(this,demux.inputPort,"In",width / 2,height))
            }
        }

        init(demux, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))
        val direction = properties.getValue(Properties.DIRECTION)

        val x = screenX.toDouble()
        val y = screenY.toDouble()
        val width = 3 * GuiUtils.BLOCK_SIZE
        val height = (component.numOutputs + 2) * GuiUtils.BLOCK_SIZE

        var zeroXOffset: Int

        fun southWest(): Int {
            graphics.beginPath()
            graphics.moveTo(x, y)
            graphics.lineTo(x + width, y + min(20.0, height * 0.2))
            graphics.lineTo(x + width, y + height - min(20.0, height * 0.2))
            graphics.lineTo(x, y + height)
            graphics.closePath()
            return 2
        }

        fun northEast(): Int {
            graphics.beginPath()
            graphics.moveTo(x + width, y)
            graphics.lineTo(x, y + min(20.0, height * 0.2))
            graphics.lineTo(x, y + height - min(20.0, height * 0.2))
            graphics.lineTo(x + width, y + height)
            graphics.closePath()
            return width - 10
        }

        when (direction) {
            Properties.Direction.SOUTH -> {
                graphics.translate(x, y)
                graphics.rotate(270.0)
                graphics.translate((-x - width), -y)
                zeroXOffset = southWest()
            }

            Properties.Direction.WEST -> zeroXOffset = southWest()

            Properties.Direction.NORTH -> {
                graphics.translate(x, y)
                graphics.rotate(270.0)
                graphics.translate((-x - width), -y)
                zeroXOffset = northEast()
            }

            Properties.Direction.EAST -> zeroXOffset = northEast()
        }

        graphics.fill = Color.WHITE
        graphics.fill()
        graphics.stroke = Color.BLACK
        graphics.stroke()

        graphics.fill = Color.DARKGRAY
        graphics.fillText("0", x + zeroXOffset, y + 13)
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Plexer", "Demux"),
                Image(DemultiplexerPeer::class.java.getResourceAsStream("/images/Demux.png")),
                Properties(), true
            )
        }
    }
}
