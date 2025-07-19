package com.ra4king.circuitsim.simulator.components.wiring

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.components.wiring.Transistor.Ports.*

/**
 * @author Roi Atalla
 */
class Transistor(name: String, isPType: Boolean) : Component(name, intArrayOf(1, 1, 1)) {

    private val enableBit = if (isPType) WireValue.State.ZERO else WireValue.State.ONE

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        if (portIndex == PORT_OUT.ordinal) return
        val enabled = state.getLastReceived(getPort(PORT_GATE)).getBit(0) == enableBit
        state.pushValue(getPort(PORT_OUT),
            if (enabled) state.getLastReceived(getPort(PORT_IN)) else Z_VALUE)
    }

    enum class Ports {
        PORT_IN, PORT_GATE, PORT_OUT
    }

    companion object {
        private val Z_VALUE = WireValue(1)
    }
}
