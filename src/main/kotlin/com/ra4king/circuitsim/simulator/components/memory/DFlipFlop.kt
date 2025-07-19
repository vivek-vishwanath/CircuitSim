package com.ra4king.circuitsim.simulator.components.memory

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue

import com.ra4king.circuitsim.simulator.components.memory.DFlipFlop.Ports.*

/**
 * @author Roi Atalla
 */
class DFlipFlop(name: String) : Component(name, IntArray(7) { 1 }) {
    private fun pushValue(state: CircuitState, bit: WireValue.State) {
        state.putComponentProperty(this, bit)
        state.pushValue(getPort(PORT_Q), WireValue(1, bit))
        state.pushValue(getPort(PORT_QN), WireValue(1, bit.negate()))
    }

    override fun init(circuitState: CircuitState, lastProperty: Any?) {
        pushValue(circuitState, if (lastProperty == null) WireValue.State.ZERO else lastProperty as WireValue.State)
    }

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        if (portIndex == PORT_Q.ordinal || portIndex == PORT_QN.ordinal) {
            return
        }

        val clear = state.getLastReceived(getPort(PORT_CLEAR)).getBit(0)
        val preset = state.getLastReceived(getPort(PORT_PRESET)).getBit(0)
        val enable = state.getLastReceived(getPort(PORT_ENABLE)).getBit(0)

        if (clear == WireValue.State.ONE) {
            pushValue(state, WireValue.State.ZERO)
        } else if (preset == WireValue.State.ONE) {
            pushValue(state, WireValue.State.ONE)
        } else if (enable != WireValue.State.ZERO && portIndex == PORT_CLOCK.ordinal && value.getBit(0) == WireValue.State.ONE) {
            val d = state.getLastReceived(getPort(PORT_D)).getBit(0)
            if (d != WireValue.State.Z) {
                pushValue(state, d)
            }
        }
    }

    enum class Ports {

        PORT_D, PORT_CLOCK, PORT_ENABLE, PORT_PRESET, PORT_CLEAR, PORT_Q, PORT_QN
    }
}
