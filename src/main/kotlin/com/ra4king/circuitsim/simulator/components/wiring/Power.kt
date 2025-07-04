package com.ra4king.circuitsim.simulator.components.wiring

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.components.wiring.Power.Ports.PORT

/**
 * @author Austin Adams and Roi Atalla
 */
class Power(name: String) : Component(name, intArrayOf(1)) {
    override fun init(circuitState: CircuitState, lastProperty: Any?) {
        circuitState.pushValue(getPort(PORT), WireValue(WireValue.State.ONE))
    }

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {}

    enum class Ports { PORT }
}
