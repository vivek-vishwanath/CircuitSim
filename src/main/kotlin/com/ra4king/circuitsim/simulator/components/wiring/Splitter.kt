package com.ra4king.circuitsim.simulator.components.wiring

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue

/**
 * @author Roi Atalla
 */
class Splitter(name: String, val bitFanIndices: IntArray) :
    Component(name, setupPortBitsizes(bitFanIndices)) {

	val portJoined: Int = numPorts - 1

    constructor(name: String, bitSize: Int, fanouts: Int) : this(name, setupBitFanIndices(bitSize, fanouts))

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        if (portIndex == portJoined) {
            check(bitFanIndices.size == value.bitSize) {
                this.toString() + ": something went wrong somewhere. bitFanIndices = " + bitFanIndices.size +
                        ", value.getBitSize() = " + value.bitSize
            }

            for (i in 0..<numPorts - 1) {
                val result = WireValue(getPort(i).link.bitSize)
                var currBit = 0
                for (j in bitFanIndices.indices) {
                    if (bitFanIndices[j] == i) {
                        result.setBit(currBit++, value.getBit(j))
                    }
                }
                state.pushValue(getPort(i), result)
            }
        } else {
            val result = WireValue(state.getLastPushed(getPort(portJoined)))
            var currBit = 0
            for (i in bitFanIndices.indices) {
                if (bitFanIndices[i] == portIndex) {
                    result.setBit(i, value.getBit(currBit++))
                }
            }

            check(currBit == value.bitSize) {
                this.toString() + ": something went wrong somewhere. currBit = " + currBit + ", value.getBitSize() = " +
                        value.bitSize
            }

            state.pushValue(getPort(portJoined), result)
        }
    }

    companion object {
        private fun setupBitFanIndices(bitSize: Int, fanouts: Int): IntArray = IntArray(bitSize) { it / ((bitSize + fanouts - 1) / fanouts) }

        private fun setupPortBitsizes(bitFanIndices: IntArray): IntArray {
            val totalFans = bitFanIndices.max()
            val fanouts = IntArray(totalFans + 2)
            fanouts[totalFans + 1] = bitFanIndices.size
            for (bitFanIndex in bitFanIndices)
                if (bitFanIndex >= 0)
                    fanouts[bitFanIndex]++
            return fanouts
        }
    }
}
