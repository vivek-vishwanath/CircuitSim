package com.ra4king.circuitsim.simulator.components.wiring

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import com.ra4king.circuitsim.simulator.components.wiring.Constant.Ports.PORT

/**
 * @author Roi Atalla
 */
class Constant(name: String, val bitSize: Int, val value: Int) : Component(name, intArrayOf(bitSize)) {

    override fun init(circuitState: CircuitState, lastProperty: Any?) {
        circuitState.pushValue(getPort(PORT), of(value.toLong(), bitSize))
    }

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {}

    enum class Ports {
        PORT
    }
}
