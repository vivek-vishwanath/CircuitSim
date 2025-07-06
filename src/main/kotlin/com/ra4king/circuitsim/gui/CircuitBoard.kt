package com.ra4king.circuitsim.gui

import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.Connection.WireConnection
import com.ra4king.circuitsim.gui.EditHistory.MoveElement
import com.ra4king.circuitsim.gui.LinkWires.Wire
import com.ra4king.circuitsim.gui.PathFinding.LocationPreference
import com.ra4king.circuitsim.simulator.Circuit
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.SimulationException
import com.ra4king.circuitsim.simulator.Simulator
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class CircuitBoard(
    name: String,
    val circuitManager: CircuitManager,
    simulator: Simulator,
    val editHistory: EditHistory
) {

    var name
        get() = circuit.name
        set(value) {
            circuit.name = value
        }
    val circuit = Circuit(name, simulator)
    var currentState = circuit.topLevelState

    val components = HashSet<ComponentPeer<*>>()
    val links = HashSet<LinkWires>()
    val badLinks = HashSet<LinkWires>()

    var moveElements: HashSet<GuiElement>? = null
    val connectedPorts = HashSet<Connection>()
    val connectionsMap = HashMap<Pair<Int, Int>, HashSet<Connection>>()

    var computeThread: Thread? = null
    var moveResult: MoveComputeResult? = null
    var addMoveAction = false

    var moveDeltaX = 0
    var moveDeltaY = 0

    var lastException: Exception? = null
        private set

    var rejoinWiresEnabled = true
        set(value) {
            field = value
            rejoinWires()
        }

    val isMoving get() = moveElements != null

    class MoveComputeResult(val wiresToAdd: HashSet<Wire>, val wiresToRemove: HashSet<Wire>, val failedConnections: HashSet<Connection>)

    override fun toString() = "CircuitBoard of $circuitManager"

    fun destroy() {
        try {
            removeElements(components)
        } catch (exc: Exception) {
            exc.printStackTrace()
        }

        try {
            removeElements(links.flatMap { it.wires }.toHashSet())
        } catch (exc: Exception) {
            exc.printStackTrace()
        }

        badLinks.clear()
        moveElements = null
        circuit.simulator.removeCircuit(circuit)
    }

    private fun updateBadLinks() {
        badLinks.clear()
        badLinks.addAll(links.filter { !it.isLinkValid })
        lastException = badLinks.firstOrNull()?.lastException
    }

    fun isValidLocation(component: ComponentPeer<*>) =
        (component.x >= 0) && (component.y >= 0) && (components +
                (moveElements?.filterIsInstance<ComponentPeer<*>>() ?: emptyList())
                ).none { it != component && it.x == component.x && it.y == component.y }

    @Synchronized
    @JvmOverloads
    fun addComponent(component: ComponentPeer<*>, splitWires: Boolean = true) {
        if (!isValidLocation(component)) throw SimulationException("Cannot place component here.")

        circuit.simulator.runSync {
            components.add(component)
            try {
                circuit.addComponent(component.component)
            } catch (e: Exception) {
                components.remove(component)
                throw e
            }

            try {
                editHistory.disable()
                if (splitWires) {
                    val toReAdd = HashSet<Wire>()
                    for (connection in component.connections) {
                        getConnections(connection.x, connection.y).forEach { attached ->
                            val linkWires = attached.linkWires
                            linkWires.addPort(connection)
                            links.add(linkWires)
                            if (attached is WireConnection) {
                                val wire = attached.parent
                                if (attached != wire.startConnection && attached != wire.endConnection)
                                    toReAdd.add(wire)
                            }
                        }
                        addConnection(connection)
                    }
                    for (wire in toReAdd) {
                        removeWire(wire)
                        addWire(wire.x, wire.y, wire.length, wire.isHorizontal)
                    }
                    rejoinWires()
                }
                updateBadLinks()
            } finally {
                editHistory.enable()
            }
        }
        editHistory.addAction(EditHistory.AddComponent(circuitManager, component))
    }

    fun updateComponent(old: ComponentPeer<*>, new: ComponentPeer<*>) {
        circuit.simulator.runSync {
            try {
                editHistory.disable()
                removeComponent(old, true)
                try {
                    circuit.updateComponent(old.component, new.component) { components.add(new) }
                } catch (e: Exception) {
                    components.remove(new)
                    throw e
                }
                addComponent(new)
            } finally {
                editHistory.enable()
                editHistory.addAction(EditHistory.UpdateComponent(circuitManager, old, new))
            }
        }
    }

    @JvmOverloads
    fun initMove(elements: HashSet<GuiElement>, remove: Boolean = true) {
        if (moveElements != null) {
            try {
                finalizeMove()
            } catch (exc: Exception) {
                circuitManager.simulatorWindow.debugUtil.logException(exc)
            }
        }

        connectedPorts.clear()
        moveElements = LinkedHashSet<GuiElement>(elements)
        addMoveAction = remove

        if (remove) {
            for (element in elements) {
                for (connection in element.connections) {
                    if ((connection is PortConnection || connection === (connection.parent as Wire).endConnection || connection === (connection.parent as Wire).startConnection)) {
                        connectedPorts.add(connection)
                    }
                }
            }

            editHistory.beginGroup()
            removeElements(elements, false)
        }
    }

    fun finalizeMove(): HashSet<GuiElement>? {
        val moveElements = this.moveElements
        if (moveElements == null) return null
        if (!addMoveAction) editHistory.beginGroup()

        val result: MoveComputeResult?

        synchronized(this) {
            if (computeThread != null) {
                computeThread!!.interrupt()
                computeThread = null
            }
            result = moveResult
            moveResult = null
        }

        val wiresToAdd = result?.wiresToAdd ?: HashSet()
        val wiresRemoved = result?.wiresToRemove ?: HashSet()
        var cannotMoveHere = false

        for (element in moveElements) {
            if (
                (element is ComponentPeer<*> && !isValidLocation(element)) ||
                (element is Wire && (element.x < 0 || element.y < 0))
            ) {
                for (e in moveElements) {
                    e.x -= moveDeltaX
                    e.y -= moveDeltaY
                }
                wiresToAdd.clear()
                wiresRemoved.clear()
                cannotMoveHere = true
                break
            }
        }
        removeElements(wiresRemoved)

        val elements = ArrayList(moveElements + wiresToAdd)
        val selectedWires = HashSet<Wire>()
        val toThrow = ArrayList<RuntimeException>()

        circuit.simulator.runSync {
            for (element in elements) {
                when (element) {
                    is ComponentPeer<*> -> try {
                        editHistory.beginGroup()
                        editHistory.addAction(MoveElement(circuitManager, element, moveDeltaX, moveDeltaY))
                        addComponent(element, true)
                    } catch (exc: java.lang.RuntimeException) {
                        editHistory.clearGroup()
                        toThrow.clear()
                        toThrow.add(exc)
                    } finally {
                        editHistory.endGroup()
                    }

                    is Wire -> try {
                        editHistory.beginGroup()
                        addWire(element.x, element.y, element.length, element.isHorizontal)
                        if (!cannotMoveHere) {
                            // Make a copy of the wire for later use
                            if (element !in wiresToAdd) {
                                selectedWires.add(Wire(null, element))
                                // Things break in undo/redo if we don't reset wires back
                                element.x -= moveDeltaX
                                element.y -= moveDeltaY
                            }
                        }
                    } catch (exc: RuntimeException) {
                        editHistory.clearGroup()
                        toThrow.clear()
                        toThrow.add(exc)
                    } finally {
                        editHistory.endGroup()
                    }
                }
            }
        }


        val newSelectedElements: HashSet<GuiElement>?

        if (!cannotMoveHere) {
            for (element in elements) {
                if (element is ComponentPeer<*>)
                // moving components doesn't actually modify the Circuit, so we must trigger the listener directly
                    circuitManager.simulatorWindow.circuitModified(circuit, element.component, true)
            }

            if (addMoveAction && moveDeltaX == 0 && moveDeltaY == 0) editHistory.clearGroup()

            newSelectedElements = HashSet()
            moveElements.forEach {
                if (it is ComponentPeer<*>) newSelectedElements.add(it)
            }
            if (!selectedWires.isEmpty()) {
                links.forEach { linkWires: LinkWires ->
                    linkWires.wires.forEach { wire: Wire ->
                        if (selectedWires.contains(wire)) {
                            newSelectedElements.add(wire)
                        } else {
                            selectedWires.forEach { selectedWire: Wire ->
                                if (selectedWire.overlaps(wire)) newSelectedElements.add(wire)
                            }
                        }
                    }
                }
            }
        } else {
            newSelectedElements = null
            if (addMoveAction) editHistory.clearGroup()
        }

        // Closes the beginGroup in initMove or at the beginning of this function
        editHistory.endGroup()

        this.moveElements = null
        wiresToAdd.clear()
        connectedPorts.clear()
        moveDeltaX = 0
        moveDeltaY = 0

        updateBadLinks()

        if (cannotMoveHere) throw SimulationException("Cannot move components/wires here.")
        if (!toThrow.isEmpty()) throw toThrow[0]

        return newSelectedElements
    }

    /**
     * Moves currently dragged elements a given distance.
     * This is
     * @param dx The number of x units to move
     * @param dy The number of y units to move
     * @param extendWires Whether the moved elements should be connected
     *     to non-dragged components.
     *
     * @implNote The elements currently dragged are defined by the `moveElements` Set.
     *     The initialization step (initMove) updates the elements of this set, and
     *     the finalization step (finalizeMove) clears this set and completes the move.
     */
    fun moveElements(dx: Int, dy: Int, extendWires: Boolean) {
        if (moveDeltaX == dx && moveDeltaY == dy) return
        if (moveElements == null) return
        for (element in moveElements!!) {
            element.x += dx - moveDeltaX
            element.y += dy - moveDeltaY
        }

        moveDeltaX = dx
        moveDeltaY = dy

        val latch = CountDownLatch(1)
        synchronized(this) {
            moveResult = null
            computeThread?.interrupt()
            if (!extendWires) {
                lastException = null
                return
            }

            val connectedPorts = this.connectedPorts.sortedWith { p1, p2 ->
                if (p1.x == p2.x) { if (dy > 0) p1.y - p2.y else p2.y - p1.y }
                else { if (dx > 0) p1.x - p2.x else p2.x - p1.x }
            }
            computeThread = Thread {
                val paths = HashSet<Wire>()
                val consumedWires = HashSet<Wire>()
                val failedConnections = HashSet<Connection>()
                val portsSeen = HashSet<Pair<Int, Int>>()

                for (connectedPort in connectedPorts) {
                    if (Thread.currentThread().isInterrupted) return@Thread
                    val (x, y) = connectedPort
                    var srcX = x - dx
                    var srcY = y - dy
                    if (!portsSeen.add(Pair(x, y))) continue

                    var cont = false
                    synchronized(this) {
                        var linkWires: LinkWires? = null
                        val otherConnections = getConnections(srcX, srcY)
                        if (otherConnections.isEmpty()) {
                            cont = true
                            return@synchronized
                        }

                        for (connection in otherConnections) {
                            when (connection) {
                                is PortConnection -> linkWires = connection.linkWires
                                is WireConnection -> {
                                    val wire = connection.parent
                                    if (connection == wire.startConnection || connection == wire.endConnection)
                                        linkWires = connection.linkWires
                                }
                            }
                        }

                        if (linkWires != null && !getConnections(srcX, srcY).any { it is PortConnection }) {
                            // The search for this is unfortunately O(n^2) (n = # of wires in link wires),
                            // but it seems like that's also true of other wire algorithms for LinkWire.
                            // Unfortunate.
                            //
                            // endConnection = where the source for pathfinding will be
                            // currentX, currentY = current location we're traversing
                            // nextConnection = the connection on the other side of the wire we're traversing
                            var currentX = srcX
                            var currentY = srcY
                            var endConnection: Connection? = null
                            val wires = HashSet(linkWires.wires)
                            while (wires.isNotEmpty()) {
                                // Find the next connection to traverse.
                                val cx = currentX
                                val cy = currentY
                                val possibleNext = wires.flatMap {
                                    val wStart = it.startConnection
                                    val wEnd = it.endConnection
                                    when {
                                        wStart.isAt(cx, cy) -> listOf(wEnd)
                                        wEnd.isAt(cx, cy) -> listOf(wStart)
                                        else -> emptyList()
                                    }
                                }.take(2)
                                if (possibleNext.size != 1) {
                                    // Either no "next" connection was found,
                                    // or multiple possible branches were found.
                                    // In either case, we can't really explore, so end here.
                                    break
                                }
                                val nextConnection = possibleNext[0]
                                wires.remove(nextConnection.parent)

                                val connections =
                                    getConnections(nextConnection.x, nextConnection.y)
                                // If connections < 2, then this is a dead end. Cancel this operation (it causes gnarly behaviors if we pathfind from the dead end).
                                // If connections > 2, this is a fork, so we have found the end, so we can stop here.
                                // If connections == 2 and the other connection is a port, then we've hit the end, so we also stop here.
                                if (connections.size < 2) {
                                    wires.addAll(linkWires.wires)
                                    endConnection = null
                                    break
                                } else if (connections.size > 2 || connections.stream()
                                        .anyMatch { c: Connection? -> c is PortConnection }
                                ) {
                                    endConnection = nextConnection
                                    break
                                } else {
                                    currentX = nextConnection.x
                                    currentY = nextConnection.y
                                }
                            }
                            if (endConnection != null) {
                                srcX = endConnection.x
                                srcY = endConnection.y
                            }

                            val traversedWires = HashSet(linkWires.wires)
                            traversedWires.removeAll(wires)
                            consumedWires.addAll(traversedWires)
                        }
                    }
                    if (cont) continue

                    // All components currently on the board or being dragged.
                    val components = HashSet(components)
                    components.addAll(moveElements!!.filterIsInstance<ComponentPeer<*>>())

                    val sx = srcX
                    val sy = srcY
                    val path = PathFinding.bestPath(sx, sy, x, y) { px: Int, py: Int, horizontal: Boolean ->
                        if (px == x && py == y) return@bestPath LocationPreference.VALID
                        if (px == sx && py == sy) return@bestPath LocationPreference.VALID

                        // All connections to (px, py) (including preexisting paths and a path found for a different port)
                        val connections = (connectedPorts + paths.flatMap { it.connections }).filter { it.isAt(px, py) }.toHashSet()
                        synchronized(this@CircuitBoard) {
                            connections.addAll(getConnections(px, py))
                        }

                        for (connection in connections) {
                            // Reject any ports connected to other components
                            // (we don't want to connect a wire to another component)
                            if (connection is PortConnection) return@bestPath LocationPreference.INVALID

                            // Reject any points that would overlap with another wire
                            // (same orientation and same point)
                            // or would cause the wire to connect to another wire
                            // (point on the edge of wire)
                            if (connection is WireConnection) {
                                val wire = connection.parent
                                if (!consumedWires.contains(wire)) {
                                    // Path overlaps with the present wire.
                                    if (wire.isHorizontal == horizontal && wire.contains(px, py)) {
                                        // This line ^^ contains a bug and can fail sometimes
                                        // (especially for ports that are in the middle of wires).
                                        // The calculation below could help (alongside a !canOverlap above)
                                        // (it basically says "if you're in the middle of a wire, you can pass through it"),
                                        // but that makes the pathfinding of two ports (e.g., those of an AND gate)
                                        // in the middle of a single wire worse.
                                        // Adding extra cost to wires that overlap only partially works because
                                        // ports moving along a wire would still have suboptimal pathfinding
                                        // after enough movement. I don't really know what to do here.

                                        // boolean canOverlap = linkWires == null
                                        // 	&& wire.contains(sx, sy)
                                        // 	&& !paths.contains(wire);

                                        return@bestPath LocationPreference.INVALID
                                    }
                                    // Path would wrongly connect with the present wire
                                    if (wire.startConnection.isAt(px, py)) return@bestPath LocationPreference.INVALID
                                    if (wire.endConnection.isAt(px, py)) return@bestPath LocationPreference.INVALID
                                }
                            }
                        }


                        // Reject any wires that would overlap with a component.
                        for (component in components) {
                            if (component.contains(px, py))
                                return@bestPath LocationPreference.INVALID
                        }
                        LocationPreference.VALID
                    }
                    if (path != null && !path.isEmpty()) paths.addAll(path)
                    else failedConnections.add(connectedPort)
                }

                synchronized(this) {
                    val toRemove = consumedWires
                    val toAdd = HashSet(paths)
                    moveResult = MoveComputeResult(toAdd, toRemove, failedConnections)
                    if (computeThread == Thread.currentThread())
                        computeThread = null
                    lastException = null
                    circuitManager.simulatorWindow.setNeedsRepaint()
                }
                latch.countDown()
            }
            computeThread?.start()
            lastException = Exception("Computing...")
        }

        try {
            latch.await(20, TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {}
    }

    @Synchronized
    @JvmOverloads
    fun removeElements(elements: HashSet<out GuiElement>, removeFromCircuit: Boolean = true) {
        circuit.simulator.runSync {
            editHistory.beginGroup()
            try {
                val wiresToRemove = HashMap<LinkWires, HashSet<Wire>>()
                val elementsToRemove = HashSet(elements)
                while (elementsToRemove.isNotEmpty()) {
                    val iterator = elementsToRemove.iterator()
                    val element = iterator.next()
                    iterator.remove()
                    when (element) {
                        is ComponentPeer<*> -> {
                            removeComponent(element, removeFromCircuit)
                            if (removeFromCircuit) circuit.removeComponent(element.component)
                        }
                        is Wire -> {
                            val toRemove = HashSet<Wire>()
                            for (i in 0..< element.length) {
                                val x = if (element.isHorizontal) element.x + i else element.x
                                val y = if (element.isHorizontal) element.y else element.y + i
                                for (conn in HashSet(getConnections(x, y))) {
                                    if (conn is WireConnection) {
                                        val w = conn.parent
                                        if (w.isHorizontal == element.isHorizontal) {
                                            if (w == element) {
                                                toRemove.add(w)
                                                break
                                            } else if (w.isWithin(element)) {
                                                elementsToRemove.addAll(spliceWire(element, w))
                                                toRemove.add(w)
                                                break
                                            } else if (element.isWithin(w)) {
                                                val linkWires = w.linkWires
                                                removeWire(w)

                                                spliceWire(w, element).forEach {
                                                    addWire(linkWires, it)
                                                }
                                                val clone = Wire(element)
                                                addWire(linkWires, clone)

                                                toRemove.add(clone)
                                                break
                                            } else if (w.overlaps(element)) {
                                                val linkWires = w.linkWires
                                                removeWire(w)

                                                val triple = spliceOverlappingWire(element, w)
                                                elementsToRemove.add(triple.first)
                                                addWire(linkWires, triple.second)
                                                addWire(linkWires, triple.third)

                                                toRemove.add(triple.second)
                                                break
                                            }
                                        }
                                    }
                                }
                            }

                            toRemove.forEach { wire ->
                                wire.connections.forEach(this::removeConnection)
                                val linkWires = wire.linkWires
                                val set = wiresToRemove[linkWires] ?: HashSet()
                                set.add(wire)
                                wiresToRemove[linkWires] = set
                            }
                        }
                    }
                }
                wiresToRemove.forEach { linkWires, wires ->
                    links.remove(linkWires)
                    links.addAll(linkWires.splitWires(wires))
                    wires.forEach {
                        editHistory.addAction(EditHistory.RemoveWire(circuitManager,
                            Wire(null, it)))
                    }
                }
                rejoinWires()
                updateBadLinks()
            } finally {
                editHistory.endGroup()
            }
        }
    }

    private fun spliceWire(toSplice: Wire, within: Wire): HashSet<Wire> {
        require(within.isWithin(toSplice)) { "toSplice must contain within" }

        val wires = HashSet<Wire>()
        if (toSplice.isHorizontal) {
            if (toSplice.x < within.x) {
                wires.add(Wire(
                    toSplice.linkWires,
                    toSplice.x,
                    toSplice.y,
                    within.x - toSplice.x,
                    true
                ))
            }

            val withinEnd = within.x + within.length
            val toSpliceEnd = toSplice.x + toSplice.length
            if (withinEnd < toSpliceEnd) {
                wires.add(Wire(
                    toSplice.linkWires,
                    withinEnd,
                    toSplice.y,
                    toSpliceEnd - withinEnd,
                    true
                ))
            }
        } else {
            if (toSplice.y < within.y) {
                wires.add(Wire(
                    toSplice.linkWires,
                    toSplice.x,
                    toSplice.y,
                    within.y - toSplice.y,
                    false
                ))
            }

            val withinEnd = within.y + within.length
            val toSpliceEnd = toSplice.y + toSplice.length
            if (withinEnd < toSpliceEnd) {
                wires.add(Wire(
                    toSplice.linkWires,
                    toSplice.x,
                    withinEnd,
                    toSpliceEnd - withinEnd,
                    false
                ))
            }
        }

        return wires
    }

    private fun spliceOverlappingWire(toSplice: Wire, overlap: Wire): Triple<Wire, Wire, Wire> {
        require(toSplice.overlaps(overlap)) { "wires must overlap" }

        if (toSplice.isHorizontal) {
            val left = if (toSplice.x < overlap.x) toSplice else overlap
            val right = if (toSplice.x < overlap.x) overlap else toSplice

            val leftPiece = Wire(left.linkWires, left.x, left.y, right.x - left.x, true)
            val midPiece =
                Wire(
                    right.linkWires,
                    right.x,
                    right.y,
                    left.x + left.length - right.x,
                    true
                )
            val rightPiece = Wire(
                right.linkWires,
                left.x + left.length,
                left.y,
                right.x + right.length - left.x - left.length,
                true
            )

            return if (left == toSplice)
                Triple(leftPiece, midPiece, rightPiece)
            else
                Triple(rightPiece, midPiece, leftPiece)
        }
        else {
            val top = if (toSplice.y < overlap.y) toSplice else overlap
            val bottom = if (toSplice.y < overlap.y) overlap else toSplice

            val topPiece = Wire(top.linkWires, top.x, top.y, bottom.y - top.y, false)
            val midPiece = Wire(
                bottom.linkWires,
                bottom.x,
                bottom.y,
                top.y + top.length - bottom.y,
                false
            )
            val bottomPiece = Wire(
                bottom.linkWires,
                top.x,
                top.y + top.length,
                bottom.y + bottom.length - top.y - top.length,
                false
            )

            return if (top == toSplice)
                Triple(topPiece, midPiece, bottomPiece)
            else
                Triple(bottomPiece, midPiece, topPiece)
        }
    }

    @Synchronized
    fun addWire(x: Int, y: Int, length: Int, horizontal: Boolean) {
        if (x < 0 || y < 0 || (horizontal && x + length < 0) || (!horizontal && y + length < 0))
            throw SimulationException("Wire cannot go into negative space.")
        if (length == 0) throw SimulationException("Length cannot be 0")

        circuit.simulator.runSync {
            try {
                editHistory.beginGroup()
                val linkWires = LinkWires()
                val wiresAdded = LinkedHashSet<Wire>()
                val toSplit = HashMap<Wire, Connection>()


                val connections = getConnections(x, y)
                for (connection in connections) {
                    handleConnection(connection, linkWires)
                    if (connection is WireConnection) {
                        val wire = connection.parent
                        if (connection !== wire.startConnection && connection !== wire.endConnection) {
                            toSplit[wire] = connection
                        }
                    }
                }

                var lastX = x
                var lastY = y

                val sign = length / abs(length)
                var i = sign
                while (abs(i) <= abs(length)) {
                    val xOff = if (horizontal) i else 0
                    val yOff = if (horizontal) 0 else i
                    val currConnection = findConnection(x + xOff, y + yOff)

                    if (currConnection != null && (i == length || currConnection is PortConnection || currConnection ===
                                (currConnection.parent as Wire).startConnection || currConnection ===
                                (currConnection.parent as Wire).endConnection)
                    ) {
                        val len = if (horizontal) currConnection.x - lastX else currConnection.y - lastY
                        val wire = Wire(linkWires, lastX, lastY, len, horizontal)
                        wireAlreadyExists(wire) ?: wiresAdded.add(wire)

                        val connections =
                            if (i == length) getConnections(x + xOff, y + yOff)
                            else mutableSetOf(currConnection)

                        for (connection in connections) {
                            val parent = connection.parent
                            if (connection is WireConnection) {
                                val connWire = parent as Wire
                                if (connection !== connWire.startConnection &&
                                    connection !== connWire.endConnection
                                ) toSplit[parent] = connection
                            }

                            handleConnection(connection, linkWires)
                        }

                        lastX = currConnection.x
                        lastY = currConnection.y
                    } else if (i == length) {
                        val len = if (horizontal) x + xOff - lastX else y + yOff - lastY
                        val wire = Wire(linkWires, lastX, lastY, len, horizontal)
                        val surrounding = wireAlreadyExists(wire)
                        if (surrounding == null) {
                            wiresAdded.add(wire)
                        }

                        lastX = x + xOff
                        lastY = y + yOff
                    }
                    i += sign
                }

                for (wire in wiresAdded) addWire(linkWires, wire)

                toSplit.forEach(this::splitWire)
                rejoinWires()
                updateBadLinks()
            } finally {
                editHistory.endGroup()
            }
        }
    }

    private fun wireAlreadyExists(wire: Wire): Wire? {
        val connections = getConnections(wire.x, wire.y)
        if (connections.isEmpty()) return null
        for (connection in connections) {
            if (connection is WireConnection && wire.isWithin(connection.parent))
                return connection.parent
        }
        return null
    }

    private fun splitWire(wire: Wire, connection: Connection) {
        val links = wire.linkWires
        removeWire(wire)
        val len = if (connection.x == wire.x) connection.y - wire.y else connection.x - wire.x
        val wire1 = Wire(links, wire.x, wire.y, len, wire.isHorizontal)
        val wire2 = Wire(links, connection.x, connection.y, wire.length - len, wire.isHorizontal)
        addWire(links, wire1)
        addWire(links, wire2)
    }

    private fun addWire(linkWires: LinkWires, wire: Wire) {
        linkWires.addWire(wire)
        links.add(linkWires)
        wire.connections.forEach(this::addConnection)
        editHistory.addAction(EditHistory.AddWire(circuitManager, wire))
    }

    private fun removeWire(wire: Wire) {
        wire.connections.forEach(this::removeConnection)
        wire.linkWires.removeWire(wire)
        editHistory.addAction(EditHistory.RemoveWire(circuitManager, Wire(null, wire)))
    }

    @Synchronized
    private fun rejoinWires() {
        if (!rejoinWiresEnabled) return
        editHistory.disable()
        try {
            for (linkWires in links) {

                val removed: MutableSet<Wire?> = java.util.HashSet<Wire?>()
                val wires = ArrayList<Wire>(linkWires.wires)
                for (i in wires.indices) {
                    val wire = wires[i]
                    if (removed.contains(wire)) continue

                    val start = wire.startConnection
                    val end = wire.endConnection

                    var x = wire.x
                    var y = wire.y
                    var length = wire.length

                    val startConnections = getConnections(start.x, start.y)
                    if (startConnections.size == 2) {
                        val startWires = startConnections
                            .filter { it !== start }.filterIsInstance<WireConnection>()
                            .map { it.parent }
                            .filter { it.isHorizontal == wire.isHorizontal }

                        if (startWires.size == 1) {
                            val startWire = startWires[0]
                            length += startWire.length

                            if (startWire.x < x) x = startWire.x
                            if (startWire.y < y) y = startWire.y

                            removeWire(startWire)
                            removed.add(startWire)
                        }
                    }

                    val endConnections = getConnections(end.x, end.y)
                    if (endConnections.size == 2) {
                        val endWires = endConnections
                            .filter { it !== end }.filterIsInstance<WireConnection>()
                            .map { it.parent }
                            .filter { it.isHorizontal == wire.isHorizontal }

                        if (endWires.size == 1) {
                            val endWire = endWires[0]
                            length += endWire.length

                            removeWire(endWire)
                            removed.add(endWire)
                        }
                    }

                    if (length != wire.length) {
                        removeWire(wire)
                        removed.add(wire)
                        val newWire = Wire(linkWires, x, y, length, wire.isHorizontal)
                        addWire(linkWires, newWire)
                        wires.add(newWire)
                    }
                }
            }
        } finally {
            editHistory.enable()
        }
    }

    private fun removeComponent(component: ComponentPeer<*>, removeFromComponentsList: Boolean) {
        if (component !in components) return
        for (connection in component.connections) {
            removeConnection(connection)
            val linkWires = connection.linkWires
            linkWires.removePort(connection)
            if (linkWires.isEmpty) {
                linkWires.clear()
                links.remove(linkWires)
            }
        }
        if (removeFromComponentsList) components.remove(component)
        editHistory.addAction(EditHistory.RemoveComponent(circuitManager, component))
    }

    private fun handleConnection(connection: Connection, linkWires: LinkWires) {
        val linksToMerge = connection.linkWires
        if (linkWires != linksToMerge) {
            links.remove(linksToMerge)
            linkWires.merge(linksToMerge)
        }
        links.add(linkWires)
    }

    fun findConnection(x: Int, y: Int) = connectionsMap[Pair(x, y)]?.firstOrNull()

    fun getConnections(x: Int, y: Int) = connectionsMap.getOrDefault(Pair(x, y), HashSet())

    fun paint(graphics: GraphicsContext, highlightLinkWires: LinkWires?) {
        components.forEach {
            if (!(moveElements?.contains(it) ?: false))
                paintComponent(graphics, currentState, it)
        }
        for (linkWires in links)
            for (wire in linkWires.wires)
                paintWire(graphics, currentState, wire, linkWires === highlightLinkWires)

        for (link in badLinks) {
            (link.ports + link.invalidPorts).forEach { port ->
                graphics.fill = Color.BLACK
                graphics.fillText(
                    port.port.link.bitSize.toString(),
                    port.screenX + 11.0,
                    port.screenY + 21.0
                )

                graphics.stroke = Color.ORANGE
                graphics.fill = Color.ORANGE
                graphics.strokeOval(port.screenX - 2.0, port.screenY - 2.0, 10.0, 10.0)
                graphics.fillText(
                    port.port.link.bitSize.toString(),
                    port.screenX + 10.0,
                    port.screenY + 20.0
                )
            }
        }

        moveElements?.let { elements ->
            graphics.save()
            try {
                graphics.globalAlpha = 0.5
                elements.forEach {
                    when (it) {
                        is ComponentPeer<*> -> paintComponent(graphics, currentState, it)
                        is Wire -> paintWire(graphics, currentState, it, false)
                    }
                }
                moveResult?.let { result ->
                    graphics.stroke = Color.RED
                    result.wiresToRemove.forEach { it.paint(graphics) }

                    graphics.fill = Color.RED
                    result.failedConnections.forEach {
                        graphics.fillOval(it.screenX.toDouble(), it.screenY.toDouble(),
                            it.screenWidth.toDouble(), it.screenHeight.toDouble())
                    }

                    graphics.stroke = Color.BLACK
                    result.wiresToAdd.forEach { it.paint(graphics) }
                }
            } finally {
                graphics.restore()
            }
        }
    }

    private fun paintComponent(graphics: GraphicsContext, state: CircuitState, component: ComponentPeer<*>) {
        graphics.save()
        try {
            component.paint(graphics, state)
        } finally {
            graphics.restore()
        }
         component.connections.forEach { it.paint(graphics, state) }
    }

    private fun paintWire(graphics: GraphicsContext, state: CircuitState, wire: Wire, highlight: Boolean) {
        graphics.save()
        try {
            wire.paint(graphics, state, if (highlight) 4.0 else 2.0)
        } finally {
            graphics.restore()
        }
        val start = wire.startConnection
        val end = wire.endConnection
        if (getConnections(start.x, start.y).size > 2) start.paint(graphics, state)
        if (getConnections(end.x, end.y).size > 2) end.paint(graphics, state)
    }

    @Synchronized
    private fun addConnection(connection: Connection) {
        val pair = Pair(connection.x, connection.y)
        val set = connectionsMap[pair] ?: HashSet()
        set.add(connection)
        connectionsMap[pair] = set
    }

    @Synchronized
    private fun removeConnection(connection: Connection) {
        val pair = Pair(connection.x, connection.y)
        val set = connectionsMap[pair]
        set?.let { s ->
            s.remove(connection)
            if (s.isEmpty()) connectionsMap.remove(pair)
        }
    }
}