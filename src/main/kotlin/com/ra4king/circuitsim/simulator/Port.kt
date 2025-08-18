package com.ra4king.circuitsim.simulator

/**
 * @author Roi Atalla
 */
open class Port(val component: Component, val portIndex: Int, bitSize: Int) {

    var link = Link(bitSize)
        private set

    init {
        link.participants.add(this)
    }

    fun linkPort(port: Port): Port {
        link.linkPort(port)
        return this
    }

    override fun toString() = "Port($component[$portIndex])"

    open class Link(open val bitSize: Int) {

		open val participants = HashSet<Port>()

        val circuit: Circuit?
            get() = participants
                .map { it.component.circuit }
                .firstOrNull { it != null }

        fun linkPort(port: Port): Link {
            if (participants.contains(port)) return this

            val circuit = this.circuit

            checkNotNull(circuit) { "Link does not belong to a circuit." }
            checkNotNull(port.link.circuit) { "Port does not belong to a circuit." }
            require(port.link.circuit === circuit) { "Links belong to different circuits." }
            require(port.link.bitSize == bitSize) { "Links have different bit sizes." }

            circuit.forEachState { state -> state.link(this, port.link) }

            val portParticipants = port.link.participants
            participants.addAll(portParticipants)

            for (p in portParticipants) p.link = this

            return this
        }

        fun unlinkPort(port: Port): Link {
            if (!participants.contains(port) || participants.size == 1)
                return this

            val circuit = this.circuit
            participants.remove(port)
            val link = Link(bitSize)
            link.participants.add(port)
            port.link = link

            circuit!!.forEachState { state -> state.unlink(this, port) }

            return this
        }

        override fun toString() = "Link[${participants.joinToString(",")}]"
    }
}
