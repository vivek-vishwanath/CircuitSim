package com.ra4king.circuitsim.simulator

import com.ra4king.circuitsim.simulator.CircuitState.LinkState.PortStateInfo

open class CircuitState private constructor(
    open val circuit: Circuit,
    private val componentProperties: HashMap<Component, Any?>,
    private val linkStates: HashMap<Port.Link, LinkState>,
    private val readOnly: Boolean
    ) {

    /**
     * Create a new CircuitState based on the given Circuit. It is added to the Circuit's list of states.
     *
     * @param circuit The Circuit which this CircuitState represents.
     */
    constructor(circuit: Circuit) : this(circuit, HashMap(), HashMap(), false) {
        circuit.addState(this)
    }

    /**
     * Clones the CircuitState for read-only usage. It is NOT added to the Circuit's list of states.
     *
     * @param state The CircuitState to clone.
     */
    private constructor(state: CircuitState) :
    this(state.circuit, HashMap(state.componentProperties), HashMap(), true) {
        state.linkStates.forEach { (link: Port.Link, linkState: LinkState) ->
            this.linkStates[link] = LinkState(linkState)
        }

    }

    fun getComponentProperty(component: Component): Any? = componentProperties[component]

    fun putComponentProperty(component: Component, property: Any?) {
        componentProperties[component] = property
    }

    fun removeComponentProperty(component: Component) = componentProperties.remove(component)

    /**
     * Get the current true value on the Link, which is the merging of all pushed values.
     *
     * @param link The Link for which the value is returned.
     * @return The value of the Link.
     */
    fun getMergedValue(link: Port.Link) = get(link).mergedValue

    /**
     * Get the last value received by this Port.
     *
     * @param port The Port for which the last received value is returned.
     * @return The last received value of the Port.
     */
    fun getLastReceived(port: Port) = WireValue(get(port.link).getLastReceived(port))

    /**
     * Get the last value pushed by this Port.
     *
     * @param port The Port for which the last pushed value is returned.
     * @return The last pushed value of the Port.
     */
    fun getLastPushed(port: Port) = WireValue(get(port.link).getLastPushed(port))

    fun isShortCircuited(link: Port.Link) = get(link).isShortCircuit

    /**
     * Resets this CircuitState, clearing all pushed and received values.
     * Each Component's `uninit(this)` then `init(this, null)` methods are called.
     */
    fun reset() {
        linkStates.putAll(linkStates.keys.associateWith { LinkState(it) })
        circuit.components.forEach { c ->
            try {
                c.uninit(this)
                c.init(this, null)
            } catch (_: Exception) {}
        }
    }

    private fun get(link: Port.Link) = linkStates.getOrPut(link) {
        requireNotNull(link.circuit) { "Link has no circuit!" }
        require(link.circuit == circuit) { "Link not from this circuit." }
        LinkState(link) }

    fun link(link1: Port.Link, link2: Port.Link) = circuit.simulator.runSync { get(link1).link(get(link2)) }

    fun unlink(link: Port.Link, port: Port) = circuit.simulator.runSync { get(link).unlink(port) }

    fun propagateSignal(link: Port.Link) {
        val linkState = get(link)

        linkState.participants.forEach { (_, info: PortStateInfo) ->
            val lastPropagated = info.lastPropagated
            val lastPushed = info.lastPushed
            if (lastPropagated != lastPushed) {
                linkState.cachedMergedValue = null
                linkState.isShortCircuited = null
                lastPropagated.set(lastPushed)
            }
        }

        linkState.propagate()
    }

    /**
     * Push a new value from the specified Port. The Simulator instance attached to the Circuit is notified.
     * An IllegalStateException is thrown if this CircuitState is read-only.
     *
     * @param port  The Port pushing the value.
     * @param value The value being pushed.
     */
    fun pushValue(port: Port, value: WireValue) {
        check(!readOnly) { "This CircuitState is read-only" }

        circuit.simulator.runSync {
            val linkState = get(port.link)
            val lastPushed = linkState.getLastPushed(port)
            if (value != lastPushed) {
                lastPushed.set(value)
                circuit.simulator.valueChanged(this, port)
            }
        }
    }

    fun ensureUnlinked(component: Component, removeLinks: Boolean) {
        for (i in 0..< component.numPorts) {
            val port = component.getPort(i)
            val link = port.link
            if (linkStates[link]?.let { it.participants.size > 1 } ?: false)
                throw RuntimeException("Must unlink port before removing it.")

            if (removeLinks) {
                linkStates.remove(link)
                circuit.simulator.linkRemoved(link)
            }
        }
    }

    internal inner class LinkState {

		val link: Port.Link
		val participants: HashMap<Port, PortStateInfo>
        var cachedMergedValue: WireValue? = null
        var isShortCircuited: Boolean? = null

        inner class PortStateInfo @JvmOverloads constructor(
            val lastPushed: WireValue = WireValue(link.bitSize),
            val lastPropagated: WireValue = WireValue(
                link.bitSize
            ),
            val lastReceived: WireValue = WireValue(link.bitSize)
        ) {
            internal constructor(info: PortStateInfo) : this(
                WireValue(info.lastPushed),
                WireValue(info.lastPropagated),
                WireValue(info.lastReceived)
            )
        }

        constructor(link: Port.Link) {
            this.link = link
            participants = HashMap<Port, PortStateInfo>()
            link.participants.forEach { port -> participants[port] = PortStateInfo() }
        }

        constructor(linkState: LinkState) {
            link = linkState.link
            participants = HashMap<Port, PortStateInfo>()
            linkState.participants.forEach { (port: Port, info: PortStateInfo) ->
                participants[port] = PortStateInfo(info)
            }
        }

        fun getLastPushed(port: Port): WireValue = participants[port]?.lastPushed ?: WireValue(link.bitSize)

        fun getLastReceived(port: Port): WireValue = participants[port]?.lastReceived ?: WireValue(link.bitSize)

        fun getIncomingValue(port: Port?): WireValue {
            val newValue = WireValue(link.bitSize)
            participants.forEach { (p: Port, info: PortStateInfo) ->
                if (p != port) newValue.merge(info.lastPropagated)
            }
            return newValue
        }

        val mergedValue: WireValue
            get() {
                cachedMergedValue?.let { return it }

                val newValue = WireValue(link.bitSize)
                participants.values.forEach {
                    info -> newValue.merge(info.lastPropagated) }

                cachedMergedValue = WireValue(newValue)
                isShortCircuited = null

                return newValue
            }

        val isShortCircuit: Boolean
            get() {
                try {
                    isShortCircuited?.let { return it }
                    this.mergedValue
                    return false.also { isShortCircuited = false }
                } catch (_: ShortCircuitException) {
                    return true.also { isShortCircuited = true }
                } catch (_: Throwable) {
                    return false.also { isShortCircuited = false }
                }
            }

        fun propagate() {
            val toNotify = HashMap<Port, WireValue>()

            var shortCircuit: ShortCircuitException? = null

            for (participantPort in participants.keys) {
                val incomingValue: WireValue
                try {
                    incomingValue = getIncomingValue(participantPort)
                } catch (e: ShortCircuitException) {
                    shortCircuit = e
                    continue
                }

                val lastReceived = getLastReceived(participantPort)
                if (lastReceived != incomingValue) {
                    lastReceived.set(incomingValue)
                    toNotify[participantPort] = incomingValue
                }
            }

            var exception: RuntimeException? = null

            for ((participantPort, incomingValue) in toNotify.entries) {
                try {
                    participantPort
                        .component
                        .valueChanged(this@CircuitState, incomingValue, participantPort.portIndex)
                } catch (e: ShortCircuitException) {
                    shortCircuit = e
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                    if (exception == null) exception = e
                }
            }

            // Component error is more important than a short circuit
            if (exception != null) throw exception
            if (shortCircuit != null) throw shortCircuit

            this.mergedValue // check for short circuit
        }

        fun link(other: LinkState) {
            if (this === other) return

            participants.putAll(other.participants)

            cachedMergedValue = null
            isShortCircuited = null
            participants.forEach { (_, info: PortStateInfo) -> info.lastPropagated.setAllBits(WireValue.State.Z) }

            linkStates.remove(other.link)
            circuit.simulator.linkRemoved(other.link)
            circuit.simulator.valueChanged(this@CircuitState, link)
        }

        fun unlink(port: Port) {
            val info = participants.remove(port)
            if (info == null) return

            cachedMergedValue = null
            isShortCircuited = null

            get(port.link).participants.put(
                port,
                PortStateInfo(
                    info.lastPushed,
                    WireValue(info.lastPushed),
                    WireValue(link.bitSize)
                )
            )

            var exception: RuntimeException? = null

            val newValue = WireValue(link.bitSize)
            if (info.lastReceived != newValue) {
                info.lastReceived.set(newValue)
                try {
                    port.component.valueChanged(this@CircuitState, newValue, port.portIndex)
                } catch (exc: RuntimeException) {
                    exception = exc
                }
            }

            if (participants.isEmpty()) {
                linkStates.remove(link)
                circuit.simulator.linkRemoved(link)
            } else {
                circuit.simulator.valueChanged(this@CircuitState, link)
            }

            if (exception != null) throw exception
        }
    }

    companion object {

        fun init(circuit: Circuit) = circuit.simulator.runSync { CircuitState(circuit) }
        fun init(state: CircuitState) = state.circuit.simulator.runSync { CircuitState(state) }
    }
}
