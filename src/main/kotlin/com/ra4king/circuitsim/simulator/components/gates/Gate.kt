package com.ra4king.circuitsim.simulator.components.gates

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.Port
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.State.*

/**
 * @author Roi Atalla
 */
abstract class Gate(
    name: String,
    val bitSize: Int,
    val numInputs: Int,
    val negateInputs: BooleanArray = BooleanArray(numInputs),
    val negateOutput: Boolean = false
) : Component(name, IntArray(numInputs + 1) { bitSize }) {

    init {
        require(negateInputs.size == numInputs) { "negateInputs array must be the same length as numInputs" }
    }

    val outPort: Port
        get() = getPort(numInputs)

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        if (portIndex == numInputs) return

        val result = WireValue(value.bitSize)
        for (bit in 0..<result.bitSize) {
            var portBit = state.getLastReceived(getPort(0)).getBit(bit)
            if (negateInputs[0]) portBit = portBit.negate()

            result.setBit(bit, portBit)
            var isX = result.getBit(bit) == Z

            for (port in 1..<numInputs) {
                portBit = state.getLastReceived(getPort(port)).getBit(bit)
                if (negateInputs[port]) portBit = portBit.negate()

                isX = isX and (portBit == Z)
                result.setBit(bit, operate(result.getBit(bit), portBit))
            }

            if (isX) {
                result.setBit(bit, Z)
            } else if (negateOutput) {
                result.setBit(
                    bit,
                    if (result.getBit(bit) == ONE) ZERO else ONE
                )
            }
        }

        state.pushValue(this.outPort, result)
    }

    protected open fun operate(acc: WireValue.State, bit: WireValue.State) = Z

    open class AndGate @JvmOverloads constructor(
        name: String,
        bitSize: Int,
        numInputs: Int,
        negateInputs: BooleanArray = BooleanArray(numInputs),
        negateOutput: Boolean = false
    ) : Gate(name, bitSize, numInputs, negateInputs, negateOutput) {

        override fun operate(acc: WireValue.State, bit: WireValue.State) =
            if (acc == ONE && bit == ONE) ONE
            else ZERO
    }

    open class OrGate @JvmOverloads constructor(
        name: String,
        bitSize: Int,
        numInputs: Int,
        negateInputs: BooleanArray = BooleanArray(numInputs),
        negateOutput: Boolean = false
    ) : Gate(name, bitSize, numInputs, negateInputs, negateOutput) {

        override fun operate(acc: WireValue.State, bit: WireValue.State) =
            if (acc == ONE || bit == ONE) ONE
            else ZERO
    }

    open class XorGate @JvmOverloads constructor(
        name: String,
        bitSize: Int,
        numInputs: Int,
        negateInputs: BooleanArray = BooleanArray(numInputs),
        negateOutput: Boolean = false
    ) : Gate(name, bitSize, numInputs, negateInputs, negateOutput) {

        override fun operate(acc: WireValue.State, bit: WireValue.State) =
            if (acc != Z && bit != Z && acc != bit) ONE
            else ZERO
    }

    class NotGate(name: String, bitSize: Int) : Gate(name, bitSize, 1, BooleanArray(1), true)

    class NandGate(name: String, bitSize: Int, numInputs: Int, negateInputs: BooleanArray = BooleanArray(numInputs)) :
        AndGate(name, bitSize, numInputs, negateInputs, true)

    class NorGate(name: String, bitSize: Int, numInputs: Int, negateInputs: BooleanArray = BooleanArray(numInputs)) :
        OrGate(name, bitSize, numInputs, negateInputs, true)

    class XnorGate(name: String, bitSize: Int, numInputs: Int, negateInputs: BooleanArray = BooleanArray(numInputs)) :
        XorGate(name, bitSize, numInputs, negateInputs, true)
}
