package com.ra4king.circuitsim.gui

import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.drawName
import com.ra4king.circuitsim.gui.GuiUtils.drawShape
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color

/**
 * @author Roi Atalla
 */
abstract class ComponentPeer<C : Component>(x: Int, y: Int, width: Int, height: Int) : GuiElement(x, y, width, height) {

    lateinit var component: C
    lateinit var properties: Properties
        private set
    override lateinit var connections: MutableList<PortConnection>

    protected fun init(component: C, properties: Properties, connections: MutableList<PortConnection>) {
        this.component = component
        this.properties = properties
        this.connections = connections
    }

    override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
        drawName(graphics, this, this.properties.getValue(Properties.LABEL_LOCATION))
        graphics.fill = Color.WHITE
        graphics.stroke = Color.BLACK
        drawShape(this) { v: Double, v1: Double, v2: Double, v3: Double -> graphics.fillRect(v, v1, v2, v3) }
        drawShape(this) { v: Double, v1: Double, v2: Double, v3: Double -> graphics.strokeRect(v, v1, v2, v3) }
    }

    override fun toString() = component.toString()
}
