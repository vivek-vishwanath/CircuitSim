package com.ra4king.circuitsim.simulator.components.plexers

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.Port
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import java.util.*

/**
 * @author Roi Atalla
 */
class Demultiplexer(name: String, val bitSize: Int, val numSelectBits: Int) : Component(name,
    IntArray((1 shl numSelectBits) + 2) { when (it) {
        1 shl numSelectBits + 1 -> bitSize
        1 shl numSelectBits -> numSelectBits
        else -> bitSize
    } }) {

	val numOutputs = 1 shl numSelectBits

    fun getOutputPort(index: Int) = getPort(index)

    val selectorPort: Port
        get() = getPort(numPorts - 2)

    val inputPort: Port
        get() = getPort(numPorts - 1)

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        val selectorPort = this.selectorPort

        if (getPort(portIndex) == selectorPort) {
            if (!value.isValidValue) {
                for (i in 0..<numOutputs) {
                    state.pushValue(getOutputPort(i), WireValue(this.bitSize))
                }
            } else {
                val selectedPort = value.value
                for (i in 0..<numOutputs) {
                    if (i == selectedPort) {
                        state.pushValue(getOutputPort(i), state.getLastReceived(this.inputPort))
                    } else {
                        state.pushValue(getOutputPort(i), of(0, this.bitSize))
                    }
                }
            }
        } else if (getPort(portIndex) == this.inputPort && state.getLastReceived(selectorPort).isValidValue) {
            val selectedPort = state.getLastReceived(selectorPort).value
            state.pushValue(getOutputPort(selectedPort), value)
        }
    }
}
