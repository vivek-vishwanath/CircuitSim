package com.ra4king.circuitsim.gui

import com.ra4king.circuitsim.gui.Connection.PortConnection
import com.ra4king.circuitsim.gui.Connection.WireConnection
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Port
import com.ra4king.circuitsim.simulator.SimulationException
import javafx.scene.canvas.GraphicsContext
import kotlin.math.abs

/**
 * @author Roi Atalla
 */
class LinkWires {

    val ports = HashSet<PortConnection>()
    val invalidPorts = HashSet<PortConnection>()
    val wires = HashSet<Wire>()

    var lastException: Exception? = null
        private set

    val isLinkValid: Boolean
        get() = invalidPorts.isEmpty()

    val isEmpty: Boolean
        get() = (ports.size + invalidPorts.size) <= 1 && wires.isEmpty()

    val link: Port.Link?
        get() = ports.firstOrNull()?.port?.link

    @Synchronized
    fun addWire(wire: Wire) {
        wire.setLinkWires(this)
        wires.add(wire)
    }

    @Synchronized
    fun removeWire(wire: Wire) {
        if (wires.remove(wire)) {
            wire.setLinkWires(null)
        }
    }

    fun splitWires(toRemove: MutableSet<Wire>): MutableSet<LinkWires> {
        toRemove.forEach { it.setLinkWires(null) }
        wires.removeAll(toRemove)

        val link = this.link
        if (link != null)
            for (port in ports)
                link.unlinkPort(port.port)

        val newLinkWires = HashSet<LinkWires>()

        while (!wires.isEmpty()) {
            val nextWire = wires.iterator().next()
            wires.remove(nextWire)
            val linkWires = findAttachedConnections(nextWire)
            linkWires.addWire(nextWire)
            newLinkWires.add(linkWires)
        }

        val portsLeft = HashSet(ports)
        portsLeft.addAll(invalidPorts)

        while (portsLeft.isNotEmpty()) {
            val linkWires = LinkWires()
            val nextPort = portsLeft.iterator().next()
            portsLeft.remove(nextPort)
            removePort(nextPort)
            linkWires.addPort(nextPort)

            val iter = portsLeft.iterator()
            while (iter.hasNext()) {
                val otherPort = iter.next()
                if (nextPort.x == otherPort.x && nextPort.y == otherPort.y) {
                    iter.remove()
                    removePort(otherPort)
                    linkWires.addPort(otherPort)
                }
            }

            if (linkWires.isEmpty) linkWires.clear()
            else newLinkWires.add(linkWires)
        }

        return newLinkWires
    }

    private fun findAttachedConnections(wire: Wire): LinkWires {
        val linkWires = LinkWires()

        val start = wire.startConnection
        val end = wire.endConnection
        val iter = wires.iterator()
        while (iter.hasNext()) {
            val w = iter.next()
            val wStart = w.startConnection
            val wEnd = w.endConnection
            if (connsEqual(start, wStart) || connsEqual(start, wEnd)
                || connsEqual(end, wStart) || connsEqual(end, wEnd)
            ) {
                linkWires.addWire(w)
                iter.remove()
            }
        }

        for (attachedWire in ArrayList<Wire>(linkWires.wires))
            linkWires.merge(findAttachedConnections(attachedWire))

        val allPorts = HashSet<PortConnection>()
        allPorts.addAll(ports)
        allPorts.addAll(invalidPorts)
        allPorts.forEach { port ->
            for (c in wire.connections) {
                if (port.x == c.x && port.y == c.y) {
                    removePort(port)
                    linkWires.addPort(port)
                    break
                }
            }
        }

        return linkWires
    }

    fun addPort(port: PortConnection) {
        port.setLinkWires(this)

        val link = this.link
        if (link != null) {
            try {
                link.linkPort(port.port)
            } catch (exc: Exception) {
                invalidPorts.add(port)
                lastException = exc
                return
            }
        }

        ports.add(port)
    }

    fun removePort(port: PortConnection) {
        if (!ports.contains(port)) {
            if (invalidPorts.remove(port)) {
                port.setLinkWires(null)
                port.port.link.unlinkPort(port.port)
            }
            return
        }
        ports.first().port.link.unlinkPort(port.port)
        ports.remove(port)
        port.setLinkWires(null)

        if (ports.isEmpty()) {
            val invalidPorts = HashSet<PortConnection>(this.invalidPorts)
            this.invalidPorts.clear()
            for (invalid in invalidPorts) addPort(invalid)
        }
    }


    fun clear() {
        (ports.toSet() + invalidPorts.toSet()).forEach { removePort(it) }
        HashSet(wires).forEach { removeWire(it) }
    }

    fun merge(other: LinkWires): LinkWires {
        if (other === this) return this
        other.ports.forEach(this::addPort)
        other.invalidPorts.forEach(this::addPort)
        other.wires.forEach(this::addWire)
        return this
    }

    class Wire(linkWires: LinkWires?, startX: Int, startY: Int, length: Int, val isHorizontal: Boolean) :
        GuiElement(
            startX, startY,
            if (isHorizontal) abs(length) else 0,
            if (isHorizontal) 0 else abs(length)
        ) {

        var linkWires = linkWires ?: LinkWires().also { it.addWire(this) }
            private set

        var length: Int = 0
            private set
        override val connections = ArrayList<Connection>()

        constructor(wire: Wire) : this(wire.linkWires, wire)

        constructor(linkWires: LinkWires?, wire: Wire) : this(
            linkWires, wire.x, wire.y,
            wire.length,
            wire.isHorizontal
        )

        fun setLinkWires(linkWires: LinkWires?) {
            this.linkWires = linkWires ?: LinkWires().also { it.addWire(this) }
        }

        init {
            this.length = when {
                length < 0 -> {
                    if (isHorizontal) x = startX + length
                    else y = startY + length
                    -length
                }
                length > 0 -> length
                else -> throw SimulationException("Length cannot be 0")
            }

            val xOffset = if (isHorizontal) 1 else 0
            val yOffset = if (isHorizontal) 0 else 1
            for (i in 0..< this.length)
                connections.add(WireConnection(this, i * xOffset, i * yOffset))
            connections.add(WireConnection(this, this.length * xOffset, this.length * yOffset))
        }

        override val screenX: Int
            get() = super.screenX - 1

        override val screenY: Int
            get() = super.screenY - 1

        override val screenWidth: Int
            get() = if (this.isHorizontal) super.screenWidth + 2 else 2

        override val screenHeight: Int
            get() = if (this.isHorizontal) 2 else super.screenHeight + 2

        fun isWithin(wire: Wire) =
            wire.isHorizontal == this.isHorizontal &&
                    this.x >= wire.x && this.x + this.width <= wire.x + wire.width &&
                    this.y >= wire.y && this.y + this.height <= wire.y + wire.height

        fun overlaps(wire: Wire) = wire.isHorizontal == this.isHorizontal &&
                (if (wire.isHorizontal) wire.y == y &&
                        !(wire.x >= x + this.length || x >= wire.x + wire.length) else wire.x == x &&
                        !(wire.y >= y + this.length || y >= wire.y + wire.length))

        val startConnection: Connection
            get() = connections[0]

        val endConnection: Connection
            get() = connections[connections.size - 1]

        override fun hashCode() = x xor (y shl 7) xor (if (this.isHorizontal) 1 shl 14 else 0) xor (length shl 15)

        override fun equals(other: Any?) =
            other is Wire && this.x == other.x && this.y == other.y &&
                    this.isHorizontal == other.isHorizontal && this.length == other.length

        override fun toString() = "Wire(x = $x, y = $y, length = ${this.length}, horizontal = ${this.isHorizontal})"

        override fun paint(graphics: GraphicsContext, circuitState: CircuitState?) {
            paint(graphics, circuitState, 2.0)
        }

        fun paint(graphics: GraphicsContext, circuitState: CircuitState?, width: Double) {
            GuiUtils.setBitColor(graphics, circuitState, linkWires)
            paint(graphics, width)
        }

        @JvmOverloads
        fun paint(graphics: GraphicsContext, width: Double = 2.0) {
            graphics.lineWidth = width
            graphics.strokeLine(
                super.screenX.toDouble(),
                super.screenY.toDouble(),
                (super.screenX + super.screenWidth).toDouble(),
                (super.screenY + super.screenHeight).toDouble()
            )
        }
    }

    companion object {
        private fun connsEqual(conn1: Connection, conn2: Connection) =
            conn1.x == conn2.x && conn1.y == conn2.y
    }
}
