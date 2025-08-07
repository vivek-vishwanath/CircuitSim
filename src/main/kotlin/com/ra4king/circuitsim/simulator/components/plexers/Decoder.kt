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
class Decoder(name: String, val numSelectBits: Int) : Component(name,
    IntArray((1 shl numSelectBits) + 1) { if (it == (1 shl numSelectBits)) numSelectBits else 1 }) {

	val numOutputs: Int = 1 shl numSelectBits

    fun getOutputPort(index: Int): Port = getPort(index)

    val selectorPort: Port
        get() = getPort(numPorts - 1)

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        val selectorPort = this.selectorPort

        if (getPort(portIndex) == selectorPort) {
            if (!value.isValidValue) {
                for (i in 0..<numOutputs) {
                    state.pushValue(getOutputPort(i), WireValue(1))
                }
            } else {
                val selectedPort = value.value
                for (i in 0..<numOutputs) {
                    if (i == selectedPort) {
                        state.pushValue(getOutputPort(i), of(1, 1))
                    } else {
                        state.pushValue(getOutputPort(i), of(0, 1))
                    }
                }
            }
        }
    }
}
