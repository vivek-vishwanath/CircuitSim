package com.ra4king.circuitsim.gui.peers.misc

import com.ra4king.circuitsim.gui.CircuitManager
import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface
import com.ra4king.circuitsim.gui.ComponentPeer
import com.ra4king.circuitsim.gui.GuiUtils
import com.ra4king.circuitsim.gui.GuiUtils.getBounds
import com.ra4king.circuitsim.gui.GuiUtils.getFont
import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.properties.PropertyValidators
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import javafx.scene.Cursor
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import java.util.*
import kotlin.math.ceil
import kotlin.math.max

/**
 * @author Roi Atalla
 */
class Text(props: Properties, x: Int, y: Int) : ComponentPeer<Component>(x, y, 2, 2) {
    private var text: String? = null
    private var lines: MutableList<String>? = null

    private fun setText(text: String) {
        this.text = text
        this.lines = mutableListOf(*text.split("\n".toRegex()).toTypedArray())

        val bounds = getBounds(getFont(13), text, false)
        width = max(2, ceil(bounds.width / GuiUtils.BLOCK_SIZE).toInt())
        height = max(2, ceil(bounds.height / GuiUtils.BLOCK_SIZE).toInt())
    }

    private var prevCursor: Cursor? = null
    private var entered = false

    override fun mouseEntered(manager: CircuitManager, state: CircuitState) {
        val scene = manager.simulatorWindow.scene
        prevCursor = scene.cursor
        scene.cursor = Cursor.TEXT

        entered = true
    }

    override fun mouseExited(manager: CircuitManager, state: CircuitState) {
        manager.simulatorWindow.scene.cursor = prevCursor

        entered = false
    }

    private var backspaceDown = false

    init {
        val properties = Properties()
        properties.ensureProperty(TEXT)
        properties.mergeIfExists(props)

        setText(properties.getValue(TEXT))

        val component: Component = object : Component(text!!, IntArray(0)) {
            override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {}
        }

        init(component, properties, ArrayList())
    }

    override fun keyPressed(
        manager: CircuitManager,
        state: CircuitState,
        keyCode: KeyCode,
        text: String
    ): Boolean {
        if (keyCode == KeyCode.BACK_SPACE) {
            backspaceDown = true
        }
        return keyCode == KeyCode.BACK_SPACE && !this.text!!.isEmpty()
    }

    override fun keyReleased(manager: CircuitManager, state: CircuitState, keyCode: KeyCode, text: String) {
        if (keyCode == KeyCode.BACK_SPACE) {
            backspaceDown = false
        }
        super.keyReleased(manager, state, keyCode, text)
    }

    override fun keyTyped(manager: CircuitManager, state: CircuitState, character: String) {
        if (character.isEmpty()) {
            if (backspaceDown && !text!!.isEmpty()) {
                val s = text!!.substring(0, text!!.length - 1)
                setText(s)
                properties.setValue(TEXT, s)
            }
        } else {
            var c = character[0]

            if (c.code == 10 || c.code == 13 || c.code >= 32 && c.code <= 126) { // line feed, carriage return, or visible ASCII char 
                if (c.code == 13) {
                    c = 10.toChar() // convert \r to \n
                }

                val s = text + c
                setText(s)
                properties.setValue(TEXT, s)
            }
        }
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        graphics.fill = Color.BLACK
        graphics.stroke = Color.BLACK

        graphics.lineWidth = 2.0

        val x = screenX
        val y = screenY
        val width = screenWidth
        val height = screenHeight

        if (text!!.isEmpty()) {
            graphics.drawImage(textImage, x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        } else {
            graphics.font = getFont(13)
            for (i in lines!!.indices) {
                val line = lines!![i]
                val bounds = getBounds(graphics.font, line, false)

                graphics.fillText(line, x + (width - bounds.width) * 0.5, (y + 15 * (i + 1)).toDouble())

                if (entered && i == lines!!.size - 1) {
                    val lx = x + (width + bounds.width) * 0.5 + 3
                    val ly = y + 15 * (i + 1.0)
                    graphics.strokeLine(lx, ly - 10, lx, ly)
                }
            }
        }
    }

    companion object {
        private var textImage: Image? = null

        @JvmStatic
        fun installComponent(manager: ComponentManagerInterface) {
            manager.addComponent(
                Pair("Misc", "Text"),
                Image(Text::class.java.getResourceAsStream("/images/Text.png")).also { textImage = it },
                Properties(), true
            )
        }

        private val TEXT = Properties.Property("Text", PropertyValidators.ANY_STRING_VALIDATOR, "")
    }
}
