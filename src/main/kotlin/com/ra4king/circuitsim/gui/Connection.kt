package com.ra4king.circuitsim.gui

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Port
import javafx.scene.canvas.GraphicsContext

/**
 * @author Roi Atalla
 */
abstract class Connection(open val parent: GuiElement, val xOffset: Int, val yOffset: Int) {

	abstract val linkWires: LinkWires

    val x get() = parent.x + this.xOffset
    val y get() = parent.y + this.yOffset

    val screenX: Int
        get() = x * GuiUtils.BLOCK_SIZE - 3

    val screenY: Int
        get() = y * GuiUtils.BLOCK_SIZE - 3

    val screenWidth = 6
    val screenHeight = 6

    /**
     * Checks if this connection is at the given circuit coordinates.
     * @param x the X coord
     * @param y the Y coord
     * @return whether this connection is at this coordinate.
     */
    fun isAt(x: Int, y: Int) = this.x == x && this.y == y

    fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        GuiUtils.setBitColor(graphics, circuitState, this.linkWires)
        graphics.fillOval(
            this.screenX.toDouble(),
            this.screenY.toDouble(),
            this.screenWidth.toDouble(),
            this.screenHeight.toDouble()
        )
    }

    class PortConnection(parent: ComponentPeer<*>, val port: Port, val name: String, x: Int, y: Int) :
        Connection(parent, x, y) {

        override var linkWires = LinkWires()
            private set

        init {
            this.linkWires.addPort(this)
        }

        constructor(parent: ComponentPeer<*>, port: Port, x: Int, y: Int) :
                this(parent, port, "", x, y)

        override val parent get() = super.parent as ComponentPeer<*>

        fun setLinkWires(linkWires: LinkWires?) {
            this.linkWires = linkWires ?:
                LinkWires().also { it.addPort(this) }
        }

        override fun toString() = "PortConnection(Port=$port, name=$name)"
    }

    class WireConnection(wire: LinkWires.Wire, x: Int, y: Int) : Connection(wire, x, y) {
        override val parent get() = super.parent as LinkWires.Wire

        override val linkWires get() = parent.linkWires
    }

    operator fun component1() = x
    operator fun component2() = y
}
