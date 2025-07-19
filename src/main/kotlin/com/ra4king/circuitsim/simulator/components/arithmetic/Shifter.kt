package com.ra4king.circuitsim.simulator.components.arithmetic

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.components.arithmetic.Shifter.Ports.*

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max

/**
 * @author Roi Atalla
 */
class Shifter(name: String, private val bitSize: Int, private val shiftType: ShiftType) :
    Component(name, intArrayOf(bitSize, getShiftBits(bitSize), bitSize)) {
    enum class ShiftType {
        LOGICAL_LEFT, LOGICAL_RIGHT, ARITHMETIC_RIGHT, ROTATE_LEFT, ROTATE_RIGHT
    }

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        if (portIndex == PORT_OUT.ordinal) {
            return
        }

        val valueIn = state.getLastReceived(getPort(PORT_IN))
        val shift = state.getLastReceived(getPort(PORT_SHIFT))

        val result = WireValue(bitSize)

        if (shift.isValidValue) {
            result.setAllBits(WireValue.State.ZERO)

            val shiftValue = shift.value

            when (shiftType) {
                ShiftType.ROTATE_LEFT -> for (i in 0..bitSize - 1)
                    result.setBit(i, valueIn.getBit((i - shiftValue + bitSize) % bitSize))

                ShiftType.LOGICAL_LEFT -> for (i in shiftValue..bitSize - 1)
                    result.setBit(i, valueIn.getBit(i - shiftValue))

                ShiftType.ROTATE_RIGHT -> for (i in 0..bitSize - 1)
                    result.setBit(i, valueIn.getBit((i + shiftValue) % bitSize))

                ShiftType.ARITHMETIC_RIGHT -> for (i in 0..bitSize - 1)
                    result.setBit(i, valueIn.getBit(
                        if (i >= bitSize - shiftValue) bitSize - 1
                        else (i + shiftValue) % bitSize
                    ))

                ShiftType.LOGICAL_RIGHT ->  for (i in 0..bitSize - 1 - shiftValue)
                    result.setBit(i, valueIn.getBit(i + shiftValue))
            }
        }

        state.pushValue(getPort(PORT_OUT), result)
    }

    companion object {
        private fun getShiftBits(bitSize: Int) = max(1, ceil(ln(bitSize.toDouble()) / ln(2.0)).toInt())
    }

    enum class Ports {
        PORT_IN, PORT_SHIFT, PORT_OUT
    }
}
