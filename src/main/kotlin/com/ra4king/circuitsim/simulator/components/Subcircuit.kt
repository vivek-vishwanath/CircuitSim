package com.ra4king.circuitsim.simulator.components

import com.ra4king.circuitsim.simulator.*
import com.ra4king.circuitsim.simulator.CircuitState.Companion.init
import com.ra4king.circuitsim.simulator.components.wiring.Pin
import com.ra4king.circuitsim.simulator.components.wiring.Pin.PinChangeListener

/**
 * @author Roi Atalla
 */
class Subcircuit private constructor(name: String, val subcircuit: Circuit, val pins: List<Pin>) :
    Component(name, IntArray(pins.size) { pins[it].bitSize }) {

    private val pinListeners = HashMap<CircuitState, MutableMap<Pin, PinChangeListener>>()

    constructor(name: String, subcircuit: Circuit) : this(name, subcircuit, subcircuit.components.filterIsInstance<Pin>())

    private fun checkCircuitLoop(circuit: Circuit) {
        if (circuit == this.circuit)
            throw SimulationException("Subcircuit loop detected.")

        for (component in circuit.components)
            if (component !== this && component is Subcircuit)
                checkCircuitLoop(component.subcircuit)
    }

    override var circuit = super.circuit
        set(value) {
            field = value
            checkCircuitLoop(subcircuit)
        }

    override fun init(circuitState: CircuitState, lastProperty: Any?) {
        val subcircuitState = init(subcircuit)
        circuitState.putComponentProperty(this, subcircuitState)

        val listeners = HashMap<Pin, PinChangeListener>()

        for (i in pins.indices) {
            val pin = pins[i]
            if (!pin.isInput) {
                val port = getPort(i)
                val listener = PinChangeListener {
                    p: Pin, state: CircuitState, value: WireValue ->
                    circuitState.pushValue(port, value)
                }
                pin.addChangeListener(subcircuitState, listener)
                listeners.put(pin, listener)
            }
        }

        pinListeners.put(subcircuitState, listeners)

        val oldState = lastProperty as CircuitState?

        for (component in subcircuit.components)
            component.init(subcircuitState, oldState?.getComponentProperty(component))

        if (oldState != null) {
            circuit!!.removeState(oldState)
        }
    }

    fun getSubcircuitState(parentState: CircuitState) = parentState.getComponentProperty(this) as CircuitState?

    override fun uninit(circuitState: CircuitState) {
        val subcircuitState = circuitState.getComponentProperty(this) as CircuitState
        circuitState.removeComponentProperty(this)
        subcircuit.components.forEach { component -> component.uninit(subcircuitState) }
        subcircuit.removeState(subcircuitState)
        val listeners = pinListeners[subcircuitState]
        listeners?.let {
            pins.forEach { pin: Pin ->
                val listener = listeners[pin]
                listener?.let { that -> pin.removeChangeListener(subcircuitState, that) }
            }
        }
    }

    fun getPort(pin: Pin): Port? {
        val index = pins.indexOf(pin)
        if (index == -1) return null
        return getPort(index)
    }

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        val subcircuitState = state.getComponentProperty(this) as CircuitState
        val pin = pins[portIndex]
        // Sometimes we get updates for pins that were just removed
        if (pin.isInput && pin.circuit != null) {
            subcircuitState.pushValue(pin.getPort(0), value)
        }
    }
}
