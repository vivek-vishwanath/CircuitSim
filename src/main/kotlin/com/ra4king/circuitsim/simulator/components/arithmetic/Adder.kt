package com.ra4king.circuitsim.simulator.components.arithmetic

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.components.arithmetic.Adder.Ports.*

/**
 * @author Roi Atalla
 */
class Adder(name: String, val bitSize: Int) : Component(name, intArrayOf(bitSize, bitSize, 1, bitSize, 1)) {
    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        if (portIndex == PORT_OUT.ordinal || portIndex == PORT_CARRY_OUT.ordinal) {
            return
        }

        if (state.getLastReceived(getPort(PORT_A)).isValidValue &&
            state.getLastReceived(getPort(PORT_B)).isValidValue &&
            state.getLastReceived(getPort(PORT_CARRY_IN)).isValidValue
        ) {
            val a = state.getLastReceived(getPort(PORT_A))
            val b = state.getLastReceived(getPort(PORT_B))
            val c = state.getLastReceived(getPort(PORT_CARRY_IN))

            val sum = WireValue(bitSize)

            var carry = if (c.getBit(0) == WireValue.State.ONE) WireValue.State.ONE else WireValue.State.ZERO
            for (i in 0..<sum.bitSize) {
                val bitA = a.getBit(i)
                val bitB = b.getBit(i)

                sum.setBit(
                    i,
                    if ((bitA == WireValue.State.ONE) xor (bitB == WireValue.State.ONE) xor (carry == WireValue.State.ONE)) WireValue.State.ONE else WireValue.State.ZERO
                )
                carry =
                    if ((bitA == WireValue.State.ONE && bitB == WireValue.State.ONE) || (bitA == WireValue.State.ONE && carry == WireValue.State.ONE) ||
                        (bitB == WireValue.State.ONE && carry == WireValue.State.ONE)
                    ) WireValue.State.ONE else WireValue.State.ZERO
            }

            state.pushValue(getPort(PORT_OUT), sum)
            state.pushValue(getPort(PORT_CARRY_OUT), WireValue(1, carry))
        } else {
            state.pushValue(getPort(PORT_OUT), WireValue(bitSize))
            state.pushValue(getPort(PORT_CARRY_OUT), WireValue(WireValue.State.Z))
        }
    }

    enum class Ports {
        PORT_A, PORT_B, PORT_CARRY_IN, PORT_OUT, PORT_CARRY_OUT
    }
}
