package com.ra4king.circuitsim.gui

import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.GuiUtils.drawShape
import com.ra4king.circuitsim.gui.GuiUtils.getCircuitCoord
import com.ra4king.circuitsim.gui.GuiUtils.getScreenCircuitCoord
import com.ra4king.circuitsim.gui.LinkWires.Wire
import com.ra4king.circuitsim.gui.SelectingState.*
import com.ra4king.circuitsim.gui.peers.SubcircuitPeer
import com.ra4king.circuitsim.simulator.Circuit
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.SimulationException
import com.ra4king.circuitsim.simulator.Simulator
import javafx.beans.property.BooleanProperty
import javafx.event.EventHandler
import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.geometry.Point2D
import javafx.scene.Node
import javafx.scene.canvas.Canvas
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.Tab
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.input.ZoomEvent
import javafx.scene.paint.Color
import javafx.scene.text.FontSmoothingType
import javafx.scene.text.Text
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Wrapper in charge of the GUI for managing a {@link CircuitBoard}.
 * This class also handles the mouse and keyboard interactions
 * that enable displaying and editing of a given circuit.
 *
 * <h3>Coordinate Systems</h3>
 *
 * Due to the nature of this class, there are 3 different coordinate systems involved in its
 * implementation. To reduce redundancy, they will be given the following names:
 * <ol>
 *     <li>
 *         <b>Circuit Coordinate System</b>: An integer coordinate system that increments in tiles.
 *         The position of ports and components are defined in this coordinate system.
 *         This coordinate system is returned by methods such as {@link GuiElement#getX()} and
 *         {@link GuiElement#getY()} and is stored as two int variables.
 *     </li>
 *     <li>
 *         <b>Canvas Coordinate System</b>: A double coordinate system representing the graphics
 *         before any transforms (particularly scaling and translating). The graphics are defined
 *         under this coordinate system. Note that 1 tile in the circuit coordinate system
 *         corresponds to {@link GuiUtils#BLOCK_SIZE} units in the canvas coordinate system.
 *         This coordinate system is returned by methods such as {@link GuiElement#getScreenX()} and
 *         {@link GuiElement#getScreenY} and is stored as two double variables or a {@link Point2D}.
 *     </li>
 *     <li>
 *         <b>Pixel Coordinate System</b>: A double coordinate system representing the graphics
 *         after all transforms (particularly scaling and translating). 1 unit in the pixel coordinate
 *         system physically corresponds to a pixel in the Scene, so any coordinate values from
 *         outside CircuitManager typically will be in this system. This system directly corresponds to
 *         the local coordinate system in JavaFX.
 *         This coordinate system is returned by methods such as {@link Canvas#getWidth()},
 *         {@link Canvas#getHeight()}, {@link MouseEvent#getX()}, {@link MouseEvent#getY()}, etc. and
 *         is stored as two double variables or a {@link Point2D}.
 *     </li>
 * </ol>
 *
 * @author Roi Atalla
 */
class CircuitManager(
    name: String,
    val simulatorWindow: CircuitSim,
    simulator: Simulator,
    val showGrid: BooleanProperty,
    val tab: Tab
) {

    val circuitBoard = CircuitBoard(name, this, simulator, simulatorWindow.editHistory)
    val circuit: Circuit get() = circuitBoard.circuit
    var name: String
        get() = circuitBoard.name
        set(value) {
            circuitBoard.name = value
        }

    var lastException: Exception? = null
    var lastExceptionTime = -1L
    val currentError: Exception?
        get() {
            if (SHOW_ERROR_DURATION < System.currentTimeMillis() - lastExceptionTime) lastException = null
            return lastException ?: circuitBoard.lastException
        }

    var currentState = IDLE

    var lastPressed: GuiElement? = null
    var lastPressedConsumed = false
    var lastPressedKeyCode: KeyCode? = null
    var lastEntered: GuiElement? = null

    var lastMousePosition = Point2D(0.0, 0.0)
    var lastMousePressed = Point2D(0.0, 0.0)
    var translateOrigin = Point2D(0.0, 0.0)
        set(value) {
            field = Point2D(min(0.0, value.x), min(0.0, value.y))
        }
    var lastMousePressedPan = Point2D(0.0, 0.0)
    var translateOriginBeforePan = Point2D(0.0, 0.0)

    var inspectLinkWires: LinkWires? = null

    var isMouseInsideCanvas = false
    var isDraggedHorizontally = false
    var isCtrlDown = false
    var isShiftDown = false
    var zoomOnScroll = false
    var usingTrackpad = false

    val dummyCircuit = Circuit("Dummy", Simulator())
    var potentialComponent: ComponentPeer<*>? = null
    var componentCreator: ComponentManager.ComponentCreator<*>? = null
    var potentialComponentProperties: Properties? = null

    var startConnection: Connection? = null
    var endConnection: Connection? = null

    var selectedElements = HashSet<GuiElement>()
        set(value) {
            mayThrow { circuitBoard.finalizeMove() }
            field.clear()
            field.addAll(value)
            updateSelectedProperties()
        }


    /**
     * @return the bounds for the circuit board (in the circuit coordinate system).
     */
    val circuitBounds: Bounds
        get() {
            val elements = HashSet<GuiElement>(selectedElements)
            elements.addAll(circuitBoard.components)
            for (links in this.circuitBoard.links) elements.addAll(links.wires)

            val minX = elements.stream().mapToInt { el: GuiElement -> el.x }.min().orElse(5) - 5
            val minY = elements.stream().mapToInt { el: GuiElement -> el.y }.min().orElse(5) - 5
            val maxX = elements.stream().mapToInt { el: GuiElement -> el.x + el.width }.max().orElse(0) + 5
            val maxY = elements.stream().mapToInt { el: GuiElement -> el.y + el.height }.max().orElse(0) + 5

            return BoundingBox(
                minX.toDouble(), minY.toDouble(),
                (maxX - minX).toDouble(), (maxY - minY).toDouble()
            )
        }
    val commonSelectedProperties
        get() = selectedElements.filterIsInstance<ComponentPeer<*>>().map { it.properties }
            .reduce(Properties::intersect)


    fun destroy() = circuitBoard.destroy()

    fun resetLastPressed() {
        lastPressed?.let {
            lastPressedKeyCode?.let { keyCode ->
                it.keyReleased(this, circuitBoard.currentState, keyCode, keyCode.name)
            } ?: it.mouseReleased(
                this, circuitBoard.currentState,
                lastMousePosition.x - it.screenX,
                lastMousePosition.y - it.screenY
            )
        }
        lastPressed = null
        lastPressedKeyCode = null
    }

    private fun reset() {
        resetLastPressed()

        currentState = IDLE

        selectedElements = HashSet()
        simulatorWindow.clearSelection()
        mayThrow { dummyCircuit.clearComponents() }
        potentialComponent = null
        isDraggedHorizontally = false
        startConnection = null
        endConnection = null
        inspectLinkWires = null

        simulatorWindow.setNeedsRepaint()
    }

    fun updateSelectedProperties() {
        val count = selectedElements.filterIsInstance<ComponentPeer<*>>().count()
        when {
            count > 1 -> simulatorWindow.setProperties("Multiple selections", commonSelectedProperties)
            count < 1 -> simulatorWindow.clearProperties()
            else -> selectedElements.filterIsInstance<ComponentPeer<*>>().randomOrNull()
                ?.let { simulatorWindow.setProperties(it) }
        }
    }

    fun modifiedSelection(componentCreator: ComponentManager.ComponentCreator<*>?, properties: Properties?) {
        this.componentCreator = componentCreator
        this.potentialComponentProperties = properties

        mayThrow { circuitBoard.finalizeMove() }
        mayThrow { dummyCircuit.clearComponents() }

        if (currentState.ordinal % 2 != 0)
            reset()

        componentCreator?.let {
            selectedElements = HashSet()
            currentState = PLACING_COMPONENT
            val potentialComponent = componentCreator.createComponent(
                properties, getCircuitCoord(lastMousePosition.x),
                getCircuitCoord(lastMousePosition.y)
            )
            mayThrow { dummyCircuit.addComponent(potentialComponent.component) }
            potentialComponent.x -= potentialComponent.width / 2
            potentialComponent.y -= potentialComponent.height / 2
            simulatorWindow.setProperties(potentialComponent)
            this.potentialComponent = potentialComponent
            return
        }

        if (properties != null && !properties.isEmpty && selectedElements.isNotEmpty()) {
            val newComponents = selectedElements.filterIsInstance<ComponentPeer<*>>().associateWith {
                ComponentManager.forClass(it.javaClass).createComponent(
                    Properties(it.properties).mergeIfExists(properties), it.x, it.y
                )
            }
            simulatorWindow.editHistory.beginGroup()
            simulatorWindow.simulator.runSync {
                newComponents.forEach { old, new -> mayThrow { circuitBoard.updateComponent(old, new) } }
            }
            simulatorWindow.editHistory.endGroup()
            selectedElements = (selectedElements.filter { it !is ComponentPeer<*> } + newComponents.values).toHashSet()
            return
        }
        currentState = IDLE
        selectedElements = HashSet()
        potentialComponent = null
        isDraggedHorizontally = false
        startConnection = null
        endConnection = null
        simulatorWindow.setNeedsRepaint()
    }

    fun updatePotentialComponent() {
        potentialComponent?.let {
            it.x = getCircuitCoord(lastMousePosition.x) - it.width / 2
            it.y = getCircuitCoord(lastMousePosition.y) - it.height / 2
            simulatorWindow.setNeedsRepaint()
        }
    }

    fun switchToCircuitState(
        state: CircuitState = circuit.topLevelState
    ) {
        circuitBoard.currentState = state
        tab.styleClass.removeAll("top-level-indicator", "nested-state-indicator")
        tab.styleClass.add(if (circuit.topLevelState == state) "top-level-indicator" else "nested-state-indicator")
    }

    /**
     * Converts a coordinate in the pixel coordinate system to the canvas coordinate system
     * (by undoing the transforms applied.)
     *
     * @param x Pixel X
     * @param y Pixel Y
     * @return the coordinate in the canvas coordinate system
     */
    private fun pixelToCanvasCoord(x: Double, y: Double) = Point2D(x, y)
        .multiply(simulatorWindow.scaleFactorInverted)
        .subtract(translateOrigin)

    fun mayThrow(runnable: () -> Unit): Boolean {
        try {
            runnable()
            if (lastException != null && SHOW_ERROR_DURATION < System.currentTimeMillis() - lastExceptionTime)
                lastException = null
            return false
        } catch (exc: SimulationException) {
            lastException = exc
            lastExceptionTime = System.currentTimeMillis()
            return true
        } catch (exc: Exception) {
            simulatorWindow.debugUtil.logException(exc)
            lastException = exc
            lastExceptionTime = System.currentTimeMillis()
            return true
        }
    }

    fun paint(canvas: Canvas?) {
        val graphics = canvas?.graphicsContext2D ?: return
        graphics.save()
        try {
            graphics.font = GuiUtils.getFont(13)
            graphics.fontSmoothingType = FontSmoothingType.LCD

            val drawGrid = showGrid.value
            graphics.fill = if (drawGrid) Color.DARKGRAY else Color.WHITE
            graphics.fillRect(0.0, 0.0, canvas.width, canvas.height)
            graphics.scale(simulatorWindow.scaleFactor, simulatorWindow.scaleFactor)
            graphics.translate(translateOrigin.x, translateOrigin.y)

            if (drawGrid) {
                var canvasStart = pixelToCanvasCoord(0.0, 0.0)
                canvasStart = canvasStart.subtract(
                    canvasStart.x % GuiUtils.BLOCK_SIZE,
                    canvasStart.y % GuiUtils.BLOCK_SIZE
                )
                val canvasEnd = pixelToCanvasCoord(canvas.width, canvas.height)


                // BG of writable region
                graphics.fill = Color.LIGHTGRAY
                val bgOriginX = max(0.0, canvasStart.x)
                val bgOriginY = max(0.0, canvasStart.y)
                val bgWidth = canvasEnd.x - bgOriginX
                val bgHeight = canvasEnd.y - bgOriginY
                graphics.fillRect(bgOriginX, bgOriginY, bgWidth, bgHeight)

                // Grid
                graphics.fill = Color.BLACK
                var i = canvasStart.x
                while (i < canvasEnd.x) {
                    var j = canvasStart.y
                    while (j < canvasEnd.y) {
                        graphics.fillRect(i, j, 1.0, 1.0)
                        j += GuiUtils.BLOCK_SIZE.toDouble()
                    }
                    i += GuiUtils.BLOCK_SIZE.toDouble()
                }
            }

            try {
                circuitBoard.paint(graphics, inspectLinkWires)
            } catch (exc: Exception) {
                simulatorWindow.debugUtil.logException(exc)
            }

            for (selectedElement in selectedElements) {
                graphics.stroke = Color.ORANGERED
                if (selectedElement is Wire) {
                    val xOff = if (selectedElement.isHorizontal) 0.0 else 1.0
                    val yOff = if (selectedElement.isHorizontal) 1.0 else 0.0

                    graphics.strokeRect(
                        selectedElement.screenX - xOff,
                        selectedElement.screenY - yOff,
                        selectedElement.screenWidth + xOff * 2,
                        selectedElement.screenHeight + yOff * 2
                    )
                } else {
                    drawShape(selectedElement, graphics::strokeRect)
                }
            }

            if (!simulatorWindow.isSimulationEnabled) {
                graphics.save()
                try {
                    graphics.stroke = Color.RED
                    simulatorWindow.simulator.runSync {
                        for (linkToUpdate in simulatorWindow.simulator.linksToUpdate) {
                            for (port in linkToUpdate.second.participants) {
                                val connection = circuitBoard.components
                                    .flatMap { it.connections }
                                    .firstOrNull { it.port == port }
                                if (connection != null) {
                                    graphics.strokeOval(
                                        (connection.screenX - 2).toDouble(),
                                        (connection.screenY - 2).toDouble(),
                                        10.0, 10.0
                                    )
                                }
                            }
                        }
                    }
                } finally {
                    graphics.restore()
                }
            }

            inspectLinkWires?.let {
                if (it.isLinkValid) it.link?.let { link ->
                    val value = try {
                        circuitBoard.currentState.getMergedValue(link).toString()
                    } catch (_: Exception) {
                        "Error"
                    }
                    val text = Text(value)
                    text.font = graphics.font
                    val bounds = text.layoutBounds
                    val x = lastMousePressed.x - bounds.width / 2 - 3
                    val y = lastMousePressed.y + 30
                    val width = bounds.width + 6
                    val height = bounds.height + 3

                    graphics.lineWidth = 1.0
                    graphics.stroke = Color.BLACK
                    graphics.fill = Color.ORANGE.brighter()
                    graphics.fillRect(x, y, width, height)
                    graphics.strokeRect(x, y, width, height)

                    graphics.fill = Color.BLACK
                    graphics.fillText(value, x + 3, y + height - 5)
                }
            }

            when (currentState) {
                IDLE, CONNECTION_SELECTED -> startConnection?.let { start ->
                    graphics.save()
                    try {
                        graphics.lineWidth = 2.0
                        graphics.stroke = Color.GREEN
                        graphics.strokeOval(start.screenX - 2.0, start.screenY - 2.0, 10.0, 10.0)
                        endConnection?.let {
                            graphics.strokeOval(it.screenX - 2.0, it.screenY - 2.0, 10.0, 10.0)
                        }
                        if (start is PortConnection) {
                            val name = start.name
                            if (!name.isEmpty()) {
                                val text = Text(name)
                                text.font = graphics.font
                                val bounds = text.layoutBounds

                                val x = start.screenX - bounds.width / 2 - 3
                                val y = (start.screenY + 30).toDouble()
                                val width = bounds.width + 6
                                val height = bounds.height + 3

                                graphics.lineWidth = 1.0
                                graphics.stroke = Color.BLACK
                                graphics.fill = Color.ORANGE.brighter()
                                graphics.fillRect(x, y, width, height)
                                graphics.strokeRect(x, y, width, height)

                                graphics.fill = Color.BLACK
                                graphics.fillText(name, x + 3, y + height - 5)
                            }
                        }
                    } finally {
                        graphics.restore()
                    }
                }
                CONNECTION_DRAGGED -> {
                    graphics.save()
                    try {
                        val start = startConnection!!
                        graphics.lineWidth = 2.0
                        graphics.stroke = Color.GREEN
                        graphics.strokeOval((start.screenX - 2).toDouble(),(start.screenY - 2).toDouble(),10.0,10.0)

                        endConnection?.let {
                            graphics.strokeOval(it.screenX - 2.0, it.screenY - 2.0, 10.0, 10.0)
                        }

                        val startX = start.screenX + start.screenWidth / 2.0
                        val startY = start.screenY + start.screenHeight / 2.0
                        val pointX = getScreenCircuitCoord(lastMousePosition.x).toDouble()
                        val pointY = getScreenCircuitCoord(lastMousePosition.y).toDouble()
                        graphics.stroke = if (isShiftDown) Color.RED else Color.BLACK
                        if (isDraggedHorizontally) {
                            graphics.strokeLine(startX,startY,pointX,startY)
                            graphics.strokeLine(pointX,startY,pointX,pointY)
                        } else {
                            graphics.strokeLine(startX,startY,startX,pointY)
                            graphics.strokeLine(startX,pointY,pointX,pointY)
                        }
                    } finally {
                        graphics.restore()
                    }
                }

                PLACING_COMPONENT -> if (isMouseInsideCanvas) potentialComponent?.let {
                    graphics.save()
                    try {
                        it.paint(graphics, dummyCircuit.topLevelState)
                    } finally {
                        graphics.restore()
                    }
                    for (connection in it.connections) {
                        graphics.save()
                        try {
                            connection.paint(graphics, dummyCircuit.topLevelState)
                        } finally {
                            graphics.restore()
                        }
                    }
                }

                BACKGROUND_DRAGGED -> if (!simulatorWindow.isClickMode) {
                    val startX = min(lastMousePressed.x, lastMousePosition.x)
                    val startY = min(lastMousePressed.y, lastMousePosition.y)
                    val width = abs(lastMousePosition.x - lastMousePressed.x)
                    val height = abs(lastMousePosition.y - lastMousePressed.y)

                    graphics.stroke = Color.GREEN.darker()
                    graphics.strokeRect(startX, startY, width, height)
                }

                else -> {}
            }
        } finally {
            graphics.restore()
        }
    }

    override fun toString() = "CircuitManager of $name"

    companion object {
        const val SHOW_ERROR_DURATION = 3000L
    }

    // Handle Events
    fun handleArrowPressed(direction: Properties.Direction) {
        val props = when {
            currentState == PLACING_COMPONENT -> potentialComponentProperties!!
            selectedElements.isNotEmpty() -> commonSelectedProperties
            else -> return
        }
        props.setValue(Properties.DIRECTION, direction)
        modifiedSelection(componentCreator, props)
    }

    fun keyPressed(e: KeyEvent) {
        if (e.code != SHIFT && lastPressed == null && selectedElements.size == 1) {
            lastPressed = selectedElements.first()
            lastPressedConsumed = lastPressed!!.keyPressed(this, circuitBoard.currentState, e.code, e.text)
        }
        if (lastPressed != null && lastPressedConsumed) return
        simulatorWindow.setNeedsRepaint()
        if (e.isShortcutDown) isCtrlDown = true
        val direction = hashMapOf(
            RIGHT to Properties.Direction.EAST,
            LEFT to Properties.Direction.WEST,
            UP to Properties.Direction.NORTH,
            DOWN to Properties.Direction.SOUTH
        )
        when (e.code) {
            RIGHT, LEFT, UP, DOWN -> if (currentState != ELEMENT_DRAGGED) {
                e.consume()
                handleArrowPressed(direction[e.code]!!)
            }
            SHIFT -> {
                if (currentState != CONNECTION_SELECTED && currentState != CONNECTION_DRAGGED)
                    simulatorWindow.isClickMode = true
                isShiftDown = true
            }
            DELETE, BACK_SPACE -> if (selectedElements.isNotEmpty()) {
                mayThrow { circuitBoard.finalizeMove() }
                mayThrow { circuitBoard.removeElements(selectedElements) }
                reset()
            }
            ESCAPE -> {
                if (currentState == ELEMENT_DRAGGED)
                    mayThrow { circuitBoard.moveElements(0, 0, false) }
                reset()
            }
            else -> {}
        }
    }

    fun keyTyped(e: KeyEvent) {
        if (selectedElements.size == 1 && !e.isShortcutDown) {
            selectedElements.first().keyTyped(this, circuitBoard.currentState, e.character)
            simulatorWindow.setNeedsRepaint()
        }
    }

    fun keyReleased(e: KeyEvent) {
        if (e.code.isModifierKey) {
            if (!e.isShortcutDown) {
                isCtrlDown = false
                simulatorWindow.setNeedsRepaint()
            }
            if (!e.isShiftDown) {
                simulatorWindow.isClickMode = false
                isShiftDown = false
                simulatorWindow.setNeedsRepaint()
            }
        }

        if (lastPressed != null && lastPressedKeyCode == e.code) {
            lastPressed!!.keyReleased(this, circuitBoard.currentState, e.code, e.text)
            lastPressed = null
            lastPressedKeyCode = null
            simulatorWindow.setNeedsRepaint()
        }
    }

    fun mousePressed(e: MouseEvent) {
        usingTrackpad = false
        if (e.button != MouseButton.PRIMARY) {
            when (currentState) {
                PLACING_COMPONENT, CONNECTION_SELECTED, CONNECTION_DRAGGED -> reset()
                else -> {}
            }
            return
        }

        lastMousePosition = pixelToCanvasCoord(e.x, e.y)
        lastMousePressed = pixelToCanvasCoord(e.x, e.y)
        lastMousePressedPan = Point2D(e.x, e.y)
        translateOriginBeforePan = translateOrigin

        when(currentState) {
            IDLE, ELEMENT_SELECTED -> {
                inspectLinkWires = null
                startConnection?.let {
                    currentState = when {
                        simulatorWindow.isClickMode -> {
                            inspectLinkWires = it.linkWires
                            IDLE
                        }
                        isCtrlDown && selectedElements.isEmpty() -> CONNECTION_DRAGGED
                        else -> CONNECTION_SELECTED
                    }
                } ?: run {
                    val clicked = (selectedElements + circuitBoard.components).firstOrNull {
                        it.containsScreenCoord(lastMousePressed.x.toInt(), lastMousePressed.y.toInt())
                    }
                    clicked?.let { selected ->
                        if (e.clickCount == 2 && selected is SubcircuitPeer) {
                            reset()
                            selected.switchToSubcircuit(this)
                        } else if (simulatorWindow.isClickMode && lastPressed == null) {
                            lastPressed = selected
                            selected.mousePressed(this, circuitBoard.currentState, lastMousePressed.x - selected.screenX, lastMousePressed.y - selected.screenY)
                        } else if (isCtrlDown) {
                            val elements = HashSet(selectedElements)
                            if (!elements.remove(selected)) elements.add(selected)
                            selectedElements = elements
                        } else if (selected !in selectedElements) {
                            selectedElements = hashSetOf(selected)
                        }
                        if (currentState == IDLE) currentState = ELEMENT_SELECTED
                    } ?: if (!isCtrlDown) reset() else {}
                }
            }
            CONNECTION_DRAGGED -> {
                addCurrentWire()
                if (isCtrlDown) {
                    val selected = circuitBoard.getConnections(
                        getCircuitCoord(lastMousePressed.x),
                        getCircuitCoord(lastMousePressed.y)
                    )
                    if (selected.isNotEmpty()) startConnection = selected.first()
                } else {
                    currentState = IDLE
                    startConnection = null
                    endConnection = null
                }
            }
            PLACING_COMPONENT -> {
                val newComponent = componentCreator!!.createComponent(potentialComponentProperties!!, potentialComponent!!.x, potentialComponent!!.y)
                mayThrow { circuitBoard.addComponent(newComponent) }
                if (!isCtrlDown) {
                    reset()
                    if (newComponent in circuitBoard.components)
                        selectedElements = hashSetOf(newComponent)
                    currentState = ELEMENT_SELECTED
                }
            }
            else -> {}
        }

        simulatorWindow.setNeedsRepaint()
    }

    fun mouseReleased(e: MouseEvent) {
        if (e.button != MouseButton.PRIMARY) return
        lastMousePosition = pixelToCanvasCoord(e.x, e.y)
        when(currentState) {
            IDLE, ELEMENT_SELECTED, ELEMENT_DRAGGED -> {
                resetLastPressed()
                mayThrow {
                    circuitBoard.finalizeMove()?.let { selectedElements = it }
                }
                currentState = IDLE
            }
            CONNECTION_SELECTED -> {
                val selected = if (isCtrlDown) HashSet(selectedElements) else HashSet()
                for (c in circuitBoard.getConnections(startConnection!!.x, startConnection!!.y)) {
                    if (!selected.remove(c.parent)) selected.add(c.parent)
                }

                selectedElements = selected
                currentState = IDLE
            }
            CONNECTION_DRAGGED -> {
                if (!selectedElements.isEmpty() || !isCtrlDown) {
                    addCurrentWire()
                    currentState = IDLE
                    startConnection = null
                    endConnection = null
                }
            }
            PLACING_COMPONENT, BACKGROUND_DRAGGED -> if (!isCtrlDown || currentState == BACKGROUND_DRAGGED) {
                if (simulatorWindow.isClickMode) {
                    translateOrigin = translateOriginBeforePan.add(e.x, e.y).subtract(lastMousePressedPan)
                }
                currentState = IDLE
            }
        }
        checkStartConnection()
        simulatorWindow.setNeedsRepaint()
    }

    fun mouseDragged(e: MouseEvent) {
        if (e.button != MouseButton.PRIMARY) return
        val prevMousePosition = lastMousePosition
        lastMousePosition = pixelToCanvasCoord(e.x, e.y)

        fun moveElements() {
            val dx = getCircuitCoord(lastMousePosition.x - lastMousePressed.x)
            val dy = getCircuitCoord(lastMousePosition.y - lastMousePressed.y)

            if (dx != 0 || dy != 0 || currentState == ELEMENT_DRAGGED) {
                currentState = ELEMENT_DRAGGED

                if (!circuitBoard.isMoving) {
                    mayThrow { circuitBoard.initMove(selectedElements) }
                }

                mayThrow { circuitBoard.moveElements(dx, dy, !isCtrlDown) }
            }
        }

        when (currentState) {
            IDLE, BACKGROUND_DRAGGED -> {
                currentState = BACKGROUND_DRAGGED

                val startX = (min(lastMousePressed.x, lastMousePosition.x)).toInt()
                val startY = (min(lastMousePressed.y, lastMousePosition.y)).toInt()
                val width = abs(lastMousePosition.x - lastMousePressed.x).toInt()
                val height = abs(lastMousePosition.y - lastMousePressed.y).toInt()

                if (!isCtrlDown) selectedElements = HashSet()

                if (simulatorWindow.isClickMode) {
                    translateOrigin = translateOriginBeforePan
                            .add(e.x, e.y)
                            .subtract(lastMousePressedPan)
                } else {
                    selectedElements = (selectedElements + circuitBoard.components + circuitBoard.links.flatMap { it.wires }).filter { it.isWithinScreenCoord(startX, startY, width, height) }.toHashSet()
                }
            }
            ELEMENT_SELECTED -> {
                if (!simulatorWindow.isClickMode) {
                    if (isCtrlDown) updatePotentialComponent()
                    else moveElements()
                }
            }
            PLACING_COMPONENT -> if (isCtrlDown) updatePotentialComponent() else moveElements()
            ELEMENT_DRAGGED -> moveElements()
            CONNECTION_SELECTED, CONNECTION_DRAGGED -> {
                currentState = CONNECTION_DRAGGED
                checkEndConnection(prevMousePosition)
            }
        }

        checkStartConnection()
        simulatorWindow.setNeedsRepaint()
    }

    fun mouseMoved(e: MouseEvent) {
        val prevMousePosition = lastMousePosition
        lastMousePosition = pixelToCanvasCoord(e.x, e.y)

        if (currentState != IDLE) simulatorWindow.setNeedsRepaint()

        updatePotentialComponent()
        checkStartConnection()
        checkEndConnection(prevMousePosition)

        if (startConnection == null &&
            (currentState == IDLE || currentState == ELEMENT_SELECTED)
        ) {
            val component = circuitBoard
                    .components.firstOrNull { c: ComponentPeer<*> ->
                        c.containsScreenCoord(
                            lastMousePosition.x.toInt(),
                            lastMousePosition.y.toInt()
                        )
                    }
            if (component != null) {
                if (component !== lastEntered) {
                    lastEntered?.mouseExited(this, circuitBoard.currentState)
                    (component.also { lastEntered = it }).mouseEntered(this, circuitBoard.currentState)
                    simulatorWindow.setNeedsRepaint()
                }
                return
            }
            return
        }

        lastEntered
            ?.mouseExited(this, circuitBoard.currentState)
            ?.also { simulatorWindow.setNeedsRepaint() }
        lastEntered = null
    }

    fun mouseEntered(e: MouseEvent) {
        isMouseInsideCanvas = true
        simulatorWindow.setNeedsRepaint()
    }

    fun mouseExited(e: MouseEvent?) {
        isMouseInsideCanvas = false
        simulatorWindow.setNeedsRepaint()
    }

    private fun addCurrentWire() {
        val endMidX = endConnection?.x ?: getCircuitCoord(lastMousePosition.x)
        val endMidY = endConnection?.y ?: getCircuitCoord(lastMousePosition.y)

        val wires = HashSet<Wire>()
        val start = this.startConnection!!

        if (endMidX - start.x != 0 && endMidY - start.y != 0) {
            simulatorWindow.editHistory.beginGroup()
            if (isDraggedHorizontally) {
                wires.add(Wire(null,start.x,start.y,endMidX - start.x,true))
                wires.add(Wire(null, endMidX, start.y, endMidY - start.y, false))
            } else {
                wires.add(Wire(null,start.x,start.y,endMidY - start.y,false))
                wires.add(Wire(null, start.x, endMidY, endMidX - start.x, true))
            }
            simulatorWindow.editHistory.endGroup()
        } else if (endMidX - start.x != 0) {
            wires.add(Wire(null,start.x,start.y,endMidX - start.x,true))
        } else if (endMidY - start.y != 0) {
            wires.add(Wire(null, endMidX, start.y, endMidY - start.y, false))
        } else {
            val connections = circuitBoard.getConnections(start.x, start.y)
            selectedElements = (
                    (if (isCtrlDown) selectedElements else HashSet()) +
                    connections.map { it.parent }
                    ).toHashSet()
        }

        if (isShiftDown) {
            mayThrow { circuitBoard.removeElements(wires) }
        } else {
            simulatorWindow.editHistory.beginGroup()
            for (w in wires) {
                mayThrow { circuitBoard.addWire(w.x, w.y, w.length, w.isHorizontal) }
            }
            simulatorWindow.editHistory.endGroup()
        }
    }

    private fun checkStartConnection() {
        if (currentState != CONNECTION_DRAGGED) {
            val selectedConns = circuitBoard.getConnections(
                    getCircuitCoord(lastMousePosition.x),
                    getCircuitCoord(lastMousePosition.y)
                )
            val selected = selectedConns.find { it is PortConnection } as PortConnection? ?: selectedConns.firstOrNull()
            if (startConnection !== selected) simulatorWindow.setNeedsRepaint()
            startConnection = selected
        }
    }

    private fun checkEndConnection(prevMousePosition: Point2D) {
        if (currentState == CONNECTION_DRAGGED) {
            val startConnection = this.startConnection!!
            val currDiffX = getCircuitCoord(lastMousePosition.x) - startConnection.x
            val prevDiffX = getCircuitCoord(prevMousePosition.x) - startConnection.x
            val currDiffY = getCircuitCoord(lastMousePosition.y) - startConnection.y
            val prevDiffY = getCircuitCoord(prevMousePosition.y) - startConnection.y

            if (currDiffX == 0 || prevDiffX == 0 || currDiffX / abs(currDiffX) != prevDiffX / abs(prevDiffX)) {
                if (isDraggedHorizontally) simulatorWindow.setNeedsRepaint()
                isDraggedHorizontally = false
            }

            if (currDiffY == 0 || prevDiffY == 0 || currDiffY / abs(currDiffY) != prevDiffY / abs(prevDiffY)) {
                if (!isDraggedHorizontally) simulatorWindow.setNeedsRepaint()
                isDraggedHorizontally = true
            }

            val connection = circuitBoard.findConnection(
                    getCircuitCoord(lastMousePosition.x),
                    getCircuitCoord(lastMousePosition.y)
                )
            if (endConnection !== connection) simulatorWindow.setNeedsRepaint()
            endConnection = connection
        }
    }

    private fun applyZoom(originX: Double, originY: Double, zoomFactor: Double) {
        // Zoom
        var zoomFactor = zoomFactor
        val oldScale: Double = simulatorWindow.scaleFactor
        zoomFactor = CircuitSim.clampScaleFactor(oldScale * zoomFactor)
        simulatorWindow.scaleFactor = zoomFactor


        // Zoom in on point.
        // Let (tx, ty) = translateOrigin.
        // Pixel (x, y) must be at the same canvas coordinate before and after zooming,
        // so we must meet the constraint (x / oldScale - tx = x / newScale - tx').
        // When you solve this constraint, you get: (tx' = tx - factor * x),
        // where factor is the value below.
        val factor = (zoomFactor - oldScale) / (zoomFactor * oldScale)
        translateOrigin = translateOrigin.subtract(factor * originX, factor * originY)
    }

    fun scrollStarted(e: ScrollEvent) {
        this.zoomOnScroll = this.isCtrlDown
        this.usingTrackpad = true
    }

    fun scroll(e: ScrollEvent) {
        val useZoom = if(usingTrackpad) zoomOnScroll else isCtrlDown
        if (useZoom) {
            if (!e.isInertia)
                applyZoom(e.x, e.y, 2.0.pow(e.deltaY / 32))
        } else {
            val delta = Point2D(e.deltaX, e.deltaY).multiply(simulatorWindow.scaleFactorInverted)
            translateOrigin = translateOrigin.add(delta)
        }
        simulatorWindow.setNeedsRepaint()
    }

    fun scrollFinished(e: ScrollEvent) {}

    fun zoom(e: ZoomEvent) {
        applyZoom(e.x, e.y, e.zoomFactor)
        simulatorWindow.setNeedsRepaint()
    }

    fun focusGained() {}

    fun focusLost() {
        isCtrlDown = false
        isShiftDown = false
        mouseExited(null)
        simulatorWindow.isClickMode = false
        resetLastPressed()
        simulatorWindow.setNeedsRepaint()
    }

    fun contextMenuRequested(e: ContextMenuEvent) {
        val menu = ContextMenu()
        val copy = MenuItem("Copy")
        val cut = MenuItem("Cut")
        val paste = MenuItem("Paste")
        val delete = MenuItem("Delete")

        copy.onAction = EventHandler { simulatorWindow.copySelectedComponents() }
        cut.onAction = EventHandler { simulatorWindow.cutSelectedComponents() }
        paste.onAction = EventHandler { simulatorWindow.pasteFromClipboard() }
        delete.onAction = EventHandler {
            mayThrow { circuitBoard.removeElements(selectedElements) }
            selectedElements = HashSet()
            reset()
        }

        circuitBoard.components.filter { it.containsScreenCoord(
            (e.x * simulatorWindow.scaleFactorInverted).roundToInt(),
            (e.y * simulatorWindow.scaleFactorInverted).roundToInt()
        ) }.randomOrNull()?.let {
            if (isCtrlDown) {
                val selected = HashSet(selectedElements)
                selected.add(it)
                selectedElements = selected
            } else if (it !in selectedElements)
                selectedElements = hashSetOf(it)
        }

        if (selectedElements.isEmpty()) menu.items.add(paste)
        else menu.items.addAll(copy, cut, paste, delete)

        if (selectedElements.size == 1)
            menu.items.addAll(selectedElements.first().getContextMenuItems(this))

        if (menu.items.isNotEmpty()) {
            val target = e.target
            if (target is Node)
                menu.show(target.scene.window, e.screenX, e.screenY)
        }
    }
}
