package com.ra4king.circuitsim.simulator.components.arithmetic

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of

import com.ra4king.circuitsim.simulator.components.arithmetic.Subtractor.Ports.*

/**
 * @author Roi Atalla
 */
class Subtractor(name: String, val bitSize: Int) : Component(name, intArrayOf(bitSize, bitSize, 1, bitSize, 1)) {
    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        if (portIndex == PORT_OUT.ordinal || portIndex == PORT_CARRY_OUT.ordinal) {
            return
        }

        if (state.getLastReceived(getPort(PORT_A)).isValidValue &&
            state.getLastReceived(getPort(PORT_B)).isValidValue &&
            state.getLastReceived(getPort(PORT_CARRY_IN)).isValidValue
        ) {
            val a = state.getLastReceived(getPort(PORT_A)).value
            val b = state.getLastReceived(getPort(PORT_B)).value
            val carry = state.getLastReceived(getPort(PORT_CARRY_IN))

            val c = if (carry.getBit(0) == WireValue.State.ONE) 1 else 0

            state.pushValue(getPort(PORT_OUT), of((a - b - c).toLong(), bitSize))
            state.pushValue(getPort(PORT_CARRY_OUT), of((if (a - b - c < 0) 1 else 0).toLong(), 1))
        } else {
            state.pushValue(getPort(PORT_OUT), WireValue(bitSize))
            state.pushValue(getPort(PORT_CARRY_OUT), WireValue(WireValue.State.Z))
        }
    }

    enum class Ports {
        PORT_A, PORT_B, PORT_CARRY_IN, PORT_OUT, PORT_CARRY_OUT
    }
}
