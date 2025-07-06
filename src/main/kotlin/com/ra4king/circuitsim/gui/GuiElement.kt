package com.ra4king.circuitsim.gui

import com.ra4king.circuitsim.simulator.CircuitState
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.MenuItem
import javafx.scene.input.KeyCode

/**
 * @author Roi Atalla
 */
abstract class GuiElement(var x: Int, var y: Int, var width: Int, var height: Int) {
    open val screenX: Int
        get() = this.x * GuiUtils.BLOCK_SIZE

    open val screenY: Int
        get() = this.y * GuiUtils.BLOCK_SIZE

    open val screenWidth: Int
        get() = this.width * GuiUtils.BLOCK_SIZE

    open val screenHeight: Int
        get() = this.height * GuiUtils.BLOCK_SIZE

    fun containsScreenCoord(x: Int, y: Int): Boolean = x >= this.screenX && x <= this.screenX + this.screenWidth && y >= this.screenY && y <= this.screenY + this.screenHeight

    fun contains(x: Int, y: Int) = x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.height

    fun contains(element: GuiElement) = contains(element.x, element.y, element.width, element.height)

    fun contains(x: Int, y: Int, width: Int, height: Int) = x >= this.x && x + width <= this.x + this.width && y >= this.y && y + height <= this.y + this.height

    fun isWithin(element: GuiElement) = isWithin(element.x, element.y, element.width, element.height)

    fun isWithinScreenCoord(x: Int, y: Int, width: Int, height: Int): Boolean {
        val screenX = this.screenX
        val screenY = this.screenY
        val screenWidth = this.screenWidth
        val screenHeight = this.screenHeight
        return screenX >= x && screenX + screenWidth <= x + width && screenY >= y && screenY + screenHeight <= y + height
    }

    fun isWithin(x: Int, y: Int, width: Int, height: Int): Boolean = this.x >= x && this.x + this.width <= x + width && this.y >= y && this.y + this.height <= y + height

    fun intersects(element: GuiElement): Boolean = intersects(element.x, element.y, element.width, element.height)

    fun intersectsScreenCoord(x: Int, y: Int, width: Int, height: Int): Boolean = !(x >= this.screenX + this.screenWidth || this.screenX >= x + width || y >= this.screenY + this.screenHeight || this.screenY >= y + height)

    fun intersects(x: Int, y: Int, width: Int, height: Int): Boolean = !(x >= this.x + this.width || this.x >= x + width || y >= this.y + this.height || this.y >= y + height)

    open fun mousePressed(manager: CircuitManager, state: CircuitState, x: Double, y: Double) {}

    open fun mouseReleased(manager: CircuitManager, state: CircuitState, x: Double, y: Double) {}

    open fun mouseEntered(manager: CircuitManager, state: CircuitState) {}

    open fun mouseExited(manager: CircuitManager, state: CircuitState) {}

    open fun keyPressed(manager: CircuitManager, state: CircuitState, keyCode: KeyCode, text: String) = false

    open fun keyTyped(manager: CircuitManager, state: CircuitState, character: String) {}

    open fun keyReleased(manager: CircuitManager, state: CircuitState, keyCode: KeyCode, text: String) {}

    open fun getContextMenuItems(circuit: CircuitManager) = mutableListOf<MenuItem>()

    abstract val connections: MutableList<out Connection>

    abstract fun paint(graphics: GraphicsContext, circuitState: CircuitState?)
}
