package com.ra4king.circuitsim.simulator.components.memory

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import com.ra4king.circuitsim.simulator.components.memory.Register.Ports.*

/**
 * @author Roi Atalla
 */
class Register(name: String, val bitSize: Int) : Component(name, intArrayOf(bitSize, 1, 1, 1, bitSize)) {
    override fun init(circuitState: CircuitState, lastProperty: Any?) {
        val value = if (lastProperty == null) of(0, bitSize) else WireValue(lastProperty as WireValue, bitSize)
        circuitState.pushValue(getPort(PORT_OUT), value)
        circuitState.putComponentProperty(this, value)
    }

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        if (portIndex == PORT_OUT.ordinal) {
            return
        }

        if (state.getLastReceived(getPort(PORT_ZERO)).getBit(0) == WireValue.State.ONE) {
            val pushValue = of(0, bitSize)
            state.pushValue(getPort(PORT_OUT), pushValue)
            state.putComponentProperty(this, pushValue)
        } else if (state.getLastReceived(getPort(PORT_ENABLE)).getBit(0) != WireValue.State.ZERO) {
            if (portIndex == PORT_CLK.ordinal && value.getBit(0) == WireValue.State.ONE) {
                val pushValue = state.getLastReceived(getPort(PORT_IN))
                state.pushValue(getPort(PORT_OUT), pushValue)
                state.putComponentProperty(this, pushValue)
            }
        }
    }

    enum class Ports {

        PORT_IN, PORT_ENABLE, PORT_CLK, PORT_ZERO, PORT_OUT
    }
}
