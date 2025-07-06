package com.ra4king.circuitsim.simulator.components.arithmetic

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.components.arithmetic.BitExtender.Ports.*

/**
 * @author Roi Atalla
 */
class BitExtender(
    name: String,
    @JvmField val inputBitSize: Int,
    @JvmField val outputBitSize: Int,
    @JvmField val extensionType: ExtensionType
) :
    Component(name, intArrayOf(inputBitSize, outputBitSize)) {

    enum class ExtensionType {
        ZERO, ONE, SIGN
    }

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        if (portIndex == PORT_IN.ordinal) {
            val extended = WireValue(value, outputBitSize)
            if (outputBitSize > inputBitSize) {
                var i = inputBitSize
                if (extensionType == ExtensionType.ONE ||
                    extensionType == ExtensionType.SIGN && extended.getBit(inputBitSize - 1) == WireValue.State.ONE) {
                    while (i < outputBitSize) {
                        extended.setBit(i, WireValue.State.ONE)
                        i++
                    }
                }
            }

            state.pushValue(getPort(PORT_OUT), extended)
        }
    }

    enum class Ports {
        PORT_IN, PORT_OUT
    }
}
