package com.ra4king.circuitsim.simulator.components.arithmetic

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import com.ra4king.circuitsim.simulator.components.arithmetic.Multiplier.Ports.*

/**
 * @author Roi Atalla
 */
class Multiplier(name: String, val bitSize: Int) : Component(name, IntArray(5) { bitSize }) {
    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        if (portIndex == PORT_OUT_LOWER.ordinal || portIndex == PORT_OUT_UPPER.ordinal) {
            return
        }

        if (state.getLastReceived(getPort(PORT_A)).isValidValue &&
            state.getLastReceived(getPort(PORT_B)).isValidValue &&
            state.getLastReceived(getPort(PORT_CARRY_IN)).isValidValue
        ) {
            val a = state.getLastReceived(getPort(PORT_A)).value.toLong() and 0xFFFFFFFFL
            val b = state.getLastReceived(getPort(PORT_B)).value.toLong() and 0xFFFFFFFFL
            val carry = state.getLastReceived(getPort(PORT_CARRY_IN))
            val c = if (carry.isValidValue) carry.value.toLong() and 0xFFFFFFFFL else 0

            val product = a * b + c
            val upper = (product ushr bitSize).toInt()

            state.pushValue(getPort(PORT_OUT_LOWER), of(product.toInt().toLong(), bitSize))
            state.pushValue(getPort(PORT_OUT_UPPER), of(upper.toLong(), bitSize))
        } else {
            state.pushValue(getPort(PORT_OUT_LOWER), WireValue(bitSize))
            state.pushValue(getPort(PORT_OUT_UPPER), WireValue(bitSize))
        }
    }

    enum class Ports {
        PORT_A, PORT_B, PORT_CARRY_IN, PORT_OUT_LOWER, PORT_OUT_UPPER
    }
}
