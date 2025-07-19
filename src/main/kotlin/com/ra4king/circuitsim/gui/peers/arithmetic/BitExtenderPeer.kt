package com.ra4king.circuitsim.gui.peers.arithmetic

import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.getBounds
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.Properties.*
import com.ra4king.circuitsim.gui.properties.PropertyListValidator
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.components.arithmetic.BitExtender
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
 * @author Roi Atalla
 */
class BitExtenderPeer(props: Properties, x: Int, y: Int) : ComponentPeer<BitExtender>(x, y, 4, 4) {
    init {
        val properties = Properties()
        properties.ensureProperty(LABEL)
        properties.ensureProperty(LABEL_LOCATION)
        properties.ensureProperty(Property("Input Bitsize", BITSIZE.validator, 1))
        properties.ensureProperty(Property("Output Bitsize", BITSIZE.validator, 1))
        properties.ensureProperty(
            Property(
                "Extension Type",
                PropertyListValidator(BitExtender.ExtensionType.entries.toTypedArray()),
                BitExtender.ExtensionType.ZERO
            )
        )
        properties.mergeIfExists(props)

        val extender = BitExtender(
            properties.getValue(LABEL),
            properties.getValue("Input Bitsize"),
            properties.getValue("Output Bitsize"),
            properties.getValue("Extension Type")
        )

        val connections = arrayListOf(
            PortConnection(this, extender.getPort(BitExtender.Ports.PORT_IN), "A", 0, 2),
            PortConnection(this, extender.getPort(BitExtender.Ports.PORT_OUT), "B", width, 2),
        )

        init(extender, properties, connections)
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        super.paint(graphics, circuitState)

        graphics.font = getFont(12, true)
        graphics.fill = Color.BLACK

        graphics.fillText(
            component.inputBitSize.toString(),
            screenX + 3.0,
            screenY + screenHeight * 0.5 + 5
        )


        val outputString = component.outputBitSize.toString()
        val outputBounds = getBounds(graphics.font, outputString)

        graphics.fillText(
            outputString,
            screenX + screenWidth - outputBounds.width - 3,
            screenY + screenHeight * 0.5 + 5
        )

        val typeString = when (component.extensionType) {
            BitExtender.ExtensionType.ZERO -> "0"
            BitExtender.ExtensionType.ONE -> "1"
            BitExtender.ExtensionType.SIGN -> "sign"
        }

        val typeBounds = getBounds(graphics.font, typeString)
        graphics.fillText(
            typeString,
            screenX + (screenWidth - typeBounds.width) * 0.5,
            screenY + typeBounds.height
        )

        graphics.font = getFont(10, true)
        val extendString = "extend"
        val extendBounds = getBounds(graphics.font, extendString)
        graphics.fillText(
            extendString,
            screenX + (screenWidth - extendBounds.width) * 0.5,
            screenY + screenHeight - 5.0
        )
    }

    companion object {
        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Arithmetic", "Bit Extender"),
                Image(BitExtenderPeer::class.java.getResourceAsStream("/images/BitExtender.png")),
                Properties(), true
            )
        }
    }
}
