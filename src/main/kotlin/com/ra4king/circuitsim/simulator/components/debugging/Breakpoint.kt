package com.ra4king.circuitsim.simulator.components.debugging

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import com.ra4king.circuitsim.simulator.components.wiring.Clock

import com.ra4king.circuitsim.simulator.components.debugging.Breakpoint.Ports.*

/**
 * @author Charles Jenkins
 */
class Breakpoint(name: String, bitSize: Int, value: Int) : Component(name, intArrayOf(bitSize, 1)) {
    private val value: WireValue = of(value.toLong(), bitSize)

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        val dataChanged = portIndex == PORT_DATA.ordinal
        val enabled = state.getLastReceived(getPort(PORT_ENABLE)).getBit(0) != WireValue.State.ZERO
        // If clock is enabled, the data value changed, and the value is the desired breakpoint value, stop the clock
        if (dataChanged && enabled && state.getLastReceived(getPort(PORT_DATA)) == this.value) {
            Clock.stopClock(circuit!!.simulator)
        }
    }

    enum class Ports {

        PORT_DATA, PORT_ENABLE
    }
}
