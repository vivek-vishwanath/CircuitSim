package com.ra4king.circuitsim.simulator.components.arithmetic

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.components.arithmetic.Comparator.Ports.*
/**
 * @author Roi Atalla
 */
class Comparator(name: String, private val bitSize: Int, private val useSignedCompare: Boolean) : Component(name, intArrayOf(bitSize, bitSize, 1, 1, 1)) {

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        val inputA = state.getLastReceived(getPort(PORT_A))
        val inputB = state.getLastReceived(getPort(PORT_B))

        if (inputA.isValidValue && inputB.isValidValue) {
            var valueA = inputA.value.toLong()
            var valueB = inputB.value.toLong()

            if (useSignedCompare) {
                valueA = valueA or if ((valueA and (0x1L shl (bitSize - 1))) != 0L) (-1L shl bitSize) else 0
                valueB = valueB or if ((valueB and (0x1L shl (bitSize - 1))) != 0L) (-1L shl bitSize) else 0
            } else {
                valueA = valueA and 0xFFFFFFFFL
                valueB = valueB and 0xFFFFFFFFL
            }

            state.pushValue(
                getPort(PORT_LT),
                WireValue(1, if (valueA < valueB) WireValue.State.ONE else WireValue.State.ZERO)
            )
            state.pushValue(
                getPort(PORT_EQ),
                WireValue(1, if (valueA == valueB) WireValue.State.ONE else WireValue.State.ZERO)
            )
            state.pushValue(
                getPort(PORT_GT),
                WireValue(1, if (valueA > valueB) WireValue.State.ONE else WireValue.State.ZERO)
            )
        } else {
            val xValue = WireValue(1, WireValue.State.Z)
            state.pushValue(getPort(PORT_LT), xValue)
            state.pushValue(getPort(PORT_EQ), xValue)
            state.pushValue(getPort(PORT_GT), xValue)
        }
    }

    enum class Ports {

        PORT_A, PORT_B, PORT_LT, PORT_EQ, PORT_GT
    }
}
