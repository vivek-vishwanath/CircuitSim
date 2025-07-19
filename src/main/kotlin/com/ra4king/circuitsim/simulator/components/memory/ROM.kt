package com.ra4king.circuitsim.simulator.components.memory

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import com.ra4king.circuitsim.simulator.components.memory.ROM.Ports.*

/**
 * @author Roi Atalla
 */
class ROM(name: String, bitSize: Int, addressBits: Int, memory: IntArray) :
    Component(name, intArrayOf(addressBits, 1, bitSize)) {
    val addressBits: Int
    val dataBits: Int
	val memory: IntArray

    private val listeners = ArrayList<(Int, Int) -> Unit>()

    init {
        require(!(addressBits > 16 || addressBits <= 0)) { "Address bits cannot be more than 16 bits." }

        this.addressBits = addressBits
        this.dataBits = bitSize
        this.memory = memory.copyOf(1 shl addressBits)
    }

    fun addMemoryListener(listener: (Int, Int) -> Unit) {
        listeners.add(listener)
    }

    fun removeMemoryListener(listener: (Int, Int) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners(address: Int, data: Int) {
        listeners.forEach { it(address, data) }
    }

    fun loadWireValue(address: Int) =
        if (address >= 0 && address < memory.size) of(load(address).toLong(), dataBits) else null

    fun load(address: Int) = memory[address]

    fun store(address: Int, value: Int) {
        memory[address] = value
        notifyListeners(address, value)
    }

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        val enabled = state.getLastReceived(getPort(PORT_ENABLE)).getBit(0) != WireValue.State.ZERO
        val address = state.getLastReceived(getPort(PORT_ADDRESS))

        if (enabled && address.isValidValue) {
            state.pushValue(getPort(PORT_DATA), loadWireValue(address.value)!!)
        } else {
            state.pushValue(getPort(PORT_DATA), WireValue(dataBits))
        }
    }

    enum class Ports {
        PORT_ADDRESS, PORT_ENABLE, PORT_DATA
    }
}
