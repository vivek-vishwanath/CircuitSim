package com.ra4king.circuitsim.simulator.components.gates

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.components.gates.ControlledBuffer.Ports.*

/**
 * @author Roi Atalla
 */
class ControlledBuffer(name: String, bitSize: Int) : Component(name, intArrayOf(bitSize, 1, bitSize)) {

    private val Z_VALUE = WireValue(bitSize)

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        if (portIndex == PORT_OUT.ordinal) return

        val drive = state.getLastReceived(getPort(PORT_ENABLE)).getBit(0) == WireValue.State.ONE
        state.pushValue(
            getPort(PORT_OUT),
            if (drive) state.getLastReceived(getPort(PORT_IN)) else Z_VALUE
        )
    }

    enum class Ports {
        PORT_IN, PORT_ENABLE, PORT_OUT
    }
}
