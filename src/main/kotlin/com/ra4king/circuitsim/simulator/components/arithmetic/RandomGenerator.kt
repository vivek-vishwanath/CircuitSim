package com.ra4king.circuitsim.simulator.components.arithmetic

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import com.ra4king.circuitsim.simulator.components.arithmetic.RandomGenerator.Ports.*

/**
 * @author Roi Atalla
 */
class RandomGenerator(name: String, private val bitSize: Int) : Component(name, intArrayOf(1, bitSize)) {
    override fun init(circuitState: CircuitState, lastProperty: Any?) {
        circuitState.pushValue(
            getPort(PORT_OUT),
            this.randomValue
        )
    }

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        if (portIndex == PORT_CLK.ordinal && value.getBit(0) == WireValue.State.ONE) {
            state.pushValue(
                getPort(PORT_OUT),
                this.randomValue
            )
        }
    }

    private val randomValue: WireValue
        get() = of((Math.random() * (1L shl bitSize)).toLong(), bitSize)

    enum class Ports {
        PORT_CLK, PORT_OUT
    }
}
