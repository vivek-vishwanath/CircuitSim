package com.ra4king.circuitsim.simulator.components.arithmetic

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import com.ra4king.circuitsim.simulator.components.arithmetic.Negator.Ports.*

/**
 * @author Roi Atalla
 */
class Negator(name: String, bitSize: Int) : Component(name, intArrayOf(bitSize, bitSize)) {
    private val xValue = WireValue(bitSize)

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        if (portIndex == PORT_OUT.ordinal) {
            return
        }

        val result: WireValue
        if (value.isValidValue) {
            result = of(-value.value.toLong(), value.bitSize)
        } else {
            result = xValue
        }

        state.pushValue(getPort(PORT_OUT), result)
    }

    enum class Ports {
        PORT_IN, PORT_OUT
    }
}
