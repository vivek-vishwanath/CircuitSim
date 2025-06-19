package com.ra4king.circuitsim.simulator.components.arithmetic

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import com.ra4king.circuitsim.simulator.components.arithmetic.Divider.Ports.*

/**
 * @author Roi Atalla
 */
class Divider(name: String, val bitSize: Int) : Component(name, IntArray(4) { bitSize}) {
    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        if (portIndex == PORT_QUOTIENT.ordinal || portIndex == PORT_REMAINDER.ordinal) {
            return
        }

        if (state.getLastReceived(getPort(PORT_DIVIDEND)).isValidValue &&
            state.getLastReceived(getPort(PORT_DIVISOR)).isValidValue
        ) {
            val a = state.getLastReceived(getPort(PORT_DIVIDEND)).value
            val b = state.getLastReceived(getPort(PORT_DIVISOR)).value

            val quotient = if (b == 0) a else a / b
            val remainder = if (b == 0) 0 else a % b

            state.pushValue(getPort(PORT_QUOTIENT), of(quotient.toLong(), bitSize))
            state.pushValue(getPort(PORT_REMAINDER), of(remainder.toLong(), bitSize))
        } else {
            state.pushValue(getPort(PORT_QUOTIENT), WireValue(bitSize))
            state.pushValue(getPort(PORT_REMAINDER), WireValue(bitSize))
        }
    }

    enum class Ports {
        PORT_DIVIDEND, PORT_DIVISOR, PORT_QUOTIENT, PORT_REMAINDER
    }
}
