package com.ra4king.circuitsim.simulator.components.plexers

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.Port
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import java.util.*

/**
 * @author Elliott Childre
 */
class PriorityEncoder(name: String, val numSelectBits: Int) : Component(name,
    IntArray((1 shl numSelectBits) + 4) {
        if (it == (1 shl numSelectBits) + 3) numSelectBits else 1
    }) {
    private var isEnabled = false

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        val out = this.outputPort
        // if enabled IN changes
        if (portIndex == 1 shl numSelectBits) {
            this.isEnabled = value.getBit(0) == WireValue.State.ONE
        }
        // The only other input Port are the indexed inputs
        if (!this.isEnabled) {
            state.pushValue(this.enabledOutPort, WireValue(1, WireValue.State.ZERO))
            state.pushValue(out, WireValue(out.link.bitSize, WireValue.State.Z))
            state.pushValue(this.groupSignalPort, WireValue(1, WireValue.State.ZERO))
            return
        }

        // Loop through the inputs
        var highest = -1
        val ports = 1 shl numSelectBits
        for (i in 0..<ports) {
            if (state.getLastReceived(getPort(i)).getBit(0) == WireValue.State.ONE ||
                (i == portIndex && value.getBit(0) == WireValue.State.ONE)
            ) {
                highest = i
            }
        }

        if (highest == -1) {
            state.pushValue(this.enabledOutPort, WireValue(1, WireValue.State.ONE))
            state.pushValue(out, WireValue(out.link.bitSize, WireValue.State.Z))
            state.pushValue(this.groupSignalPort, WireValue(1, WireValue.State.ZERO))
        } else {
            state.pushValue(this.enabledOutPort, WireValue(1, WireValue.State.ZERO))
            state.pushValue(this.groupSignalPort, WireValue(1, WireValue.State.ONE))
            state.pushValue(this.outputPort, of(highest.toLong(), out.link.bitSize))
        }
    }

    val enabledInPort: Port
        get() = getPort(1 shl numSelectBits)

    val enabledOutPort: Port
        get() = getPort((1 shl numSelectBits) + 1)

    val groupSignalPort: Port
        get() = getPort((1 shl numSelectBits) + 2)

    val outputPort: Port
        get() = getPort((1 shl numSelectBits) + 3)
}
