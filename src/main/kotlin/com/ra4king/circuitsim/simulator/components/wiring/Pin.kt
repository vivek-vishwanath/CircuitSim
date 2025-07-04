package com.ra4king.circuitsim.simulator.components.wiring

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.Port
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import com.ra4king.circuitsim.simulator.components.wiring.Pin.Ports.PORT

/**
 * @author Roi Atalla
 */
class Pin(name: String, val bitSize: Int, val isInput: Boolean) : Component(name, IntArray(1) { bitSize }) {
    private val pinChangeListeners = HashMap<CircuitState, MutableSet<PinChangeListener>>()

    val port: Port
        get() = getPort(PORT)

    fun addChangeListener(state: CircuitState, listener: PinChangeListener) {
        pinChangeListeners.computeIfAbsent(state) { HashSet() }.add(listener)
    }

    fun removeChangeListener(state: CircuitState?, listener: PinChangeListener?) {
        pinChangeListeners[state]?.remove(listener)
    }

    fun setValue(state: CircuitState, value: WireValue) {
        state.pushValue(getPort(PORT), value)
    }

    override fun init(circuitState: CircuitState, lastProperty: Any?) {
        val c = circuit
        if (c != null && isInput && c.topLevelState == circuitState) {
            circuitState.pushValue(getPort(PORT), of(0, this.bitSize))
        }
    }

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        val listeners = pinChangeListeners.get(state)
        if (listeners != null) {
            for (listener in listeners) {
                listener.valueChanged(this, state, value)
            }
        }
    }

    fun interface PinChangeListener {
        fun valueChanged(pin: Pin, state: CircuitState, value: WireValue)
    }

    enum class Ports { PORT }
}
