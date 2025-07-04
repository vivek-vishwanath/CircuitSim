package com.ra4king.circuitsim.gui.peers.plexers

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.rotateElementSize
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.plexers.Decoder
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import kotlin.math.min

/**
 * @author Roi Atalla
 */
class DecoderPeer(props: Properties, x: Int, y: Int) : ComponentPeer<Decoder>(x, y, 3, 0) {
    init {
        val properties = Properties()
        properties.ensureProperty(Properties.LABEL)
        properties.ensureProperty(Properties.LABEL_LOCATION)
        properties.ensureProperty(Properties.DIRECTION)
        properties.ensureProperty(Properties.SELECTOR_LOCATION)
        properties.ensureProperty(Properties.SELECTOR_BITS)
        properties.mergeIfExists(props)

        val decoder = Decoder(properties.getValue(Properties.LABEL), properties.getValue(Properties.SELECTOR_BITS))
        height = decoder.numOutputs + 2

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
                while (i < decoder.numOutputs) {
                    connections.add(PortConnection(this, decoder.getPort(i), i.toString(), outOffset, i + 1))
                    i++
                }
                connections.add(
                    PortConnection(
                        this,
                        decoder.selectorPort,
                        "Selector",
                        width / 2 + selOffset,
                        if (location) 0 else height
                    )
                )
            }

            Properties.Direction.WEST -> {
                var i = 0
                while (i < decoder.numOutputs) {
                    connections.add(PortConnection(this, decoder.getPort(i), i.toString(), outOffset, i + 1))
                    i++
                }
                connections.add(
                    PortConnection(
                        this,
                        decoder.selectorPort,
                        "Selector",
                        width / 2,
                        if (location) 0 else height
                    )
                )
            }

            Properties.Direction.SOUTH -> {
                outOffset = height
                selOffset = 1
                var i = 0
                while (i < decoder.numOutputs) {
                    connections.add(PortConnection(this, decoder.getPort(i), i.toString(), i + 1, outOffset))
                    i++
                }
                connections.add(
                    PortConnection(
                        this,
                        decoder.selectorPort,
                        "Selector",
                        if (location) 0 else width,
                        height / 2 + selOffset
                    )
                )
            }

            Properties.Direction.NORTH -> {
                var i = 0
                while (i < decoder.numOutputs) {
                    connections.add(PortConnection(this, decoder.getPort(i), i.toString(), i + 1, outOffset))
                    i++
                }
                connections.add(
                    PortConnection(
                        this,
                        decoder.selectorPort,
                        "Selector",
                        if (location) 0 else width,
                        height / 2
                    )
                )
            }
        }

        init(decoder, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, properties.getValue(Properties.LABEL_LOCATION))
        val direction = properties.getValue(Properties.DIRECTION)

        val x = screenX
        val y = screenY
        val width = 3 * GuiUtils.BLOCK_SIZE
        val height = (component.numOutputs + 2) * GuiUtils.BLOCK_SIZE

        var zeroXOffset: Int

        when (direction) {
            Properties.Direction.SOUTH -> {
                graphics.translate(x.toDouble(), y.toDouble())
                graphics.rotate(270.0)
                graphics.translate((-x - width).toDouble(), -y.toDouble())
                zeroXOffset = 2
                graphics.beginPath()
                graphics.moveTo(x.toDouble(), y.toDouble())
                graphics.lineTo((x + width).toDouble(), y + min(20.0, height * 0.2))
                graphics.lineTo((x + width).toDouble(), y + height - min(20.0, height * 0.2))
                graphics.lineTo(x.toDouble(), (y + height).toDouble())
                graphics.closePath()
            }

            Properties.Direction.WEST -> {
                zeroXOffset = 2
                graphics.beginPath()
                graphics.moveTo(x.toDouble(), y.toDouble())
                graphics.lineTo((x + width).toDouble(), y + min(20.0, height * 0.2))
                graphics.lineTo((x + width).toDouble(), y + height - min(20.0, height * 0.2))
                graphics.lineTo(x.toDouble(), (y + height).toDouble())
                graphics.closePath()
            }

            Properties.Direction.NORTH -> {
                graphics.translate(x.toDouble(), y.toDouble())
                graphics.rotate(270.0)
                graphics.translate((-x - width).toDouble(), -y.toDouble())
                zeroXOffset = width - 10
                graphics.beginPath()
                graphics.moveTo((x + width).toDouble(), y.toDouble())
                graphics.lineTo(x.toDouble(), y + min(20.0, height * 0.2))
                graphics.lineTo(x.toDouble(), y + height - min(20.0, height * 0.2))
                graphics.lineTo((x + width).toDouble(), (y + height).toDouble())
                graphics.closePath()
            }

            Properties.Direction.EAST -> {
                zeroXOffset = width - 10
                graphics.beginPath()
                graphics.moveTo((x + width).toDouble(), y.toDouble())
                graphics.lineTo(x.toDouble(), y + min(20.0, height * 0.2))
                graphics.lineTo(x.toDouble(), y + height - min(20.0, height * 0.2))
                graphics.lineTo((x + width).toDouble(), (y + height).toDouble())
                graphics.closePath()
            }
        }

        graphics.fill = Color.WHITE
        graphics.fill()
        graphics.stroke = Color.BLACK
        graphics.stroke()

        graphics.fill = Color.DARKGRAY
        graphics.fillText("0", (x + zeroXOffset).toDouble(), (y + 13).toDouble())
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Plexer", "Decoder"),
                Image(DecoderPeer::class.java.getResourceAsStream("/images/Decoder.png")),
                Properties(), true
            )
        }
    }
}
