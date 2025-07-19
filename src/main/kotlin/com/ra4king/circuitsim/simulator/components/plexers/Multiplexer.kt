package com.ra4king.circuitsim.simulator.components.plexers

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.Port
import com.ra4king.circuitsim.simulator.WireValue

/**
 * @author Roi Atalla
 */
class Multiplexer(name: String, val bitSize: Int, val numSelectBits: Int) : Component(name,
    IntArray((1 shl numSelectBits) + 2) { when (it) {
        1 shl numSelectBits + 1 -> bitSize
        1 shl numSelectBits -> numSelectBits
        else -> bitSize
    } }) {

	val numInputs = 1 shl numSelectBits

    val selectorPort: Port
        get() = getPort(numPorts - 2)

    val outPort: Port
        get() = getPort(numPorts - 1)

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        val selectorPort = this.selectorPort
        val currentSelect = state.getLastReceived(selectorPort)

        if (getPort(portIndex) == selectorPort) {
            if (!value.isValidValue || !state.getLastReceived(getPort(value.value)).isValidValue) {
                state.pushValue(this.outPort, WireValue(this.bitSize))
            } else {
                state.pushValue(this.outPort, state.getLastReceived(getPort(value.value)))
            }
        } else if (portIndex < numPorts - 2) {
            if (currentSelect.isValidValue) {
                if (currentSelect.value == portIndex) {
                    state.pushValue(this.outPort, value)
                }
            } else {
                state.pushValue(this.outPort, WireValue(this.bitSize))
            }
        }
    }
}
