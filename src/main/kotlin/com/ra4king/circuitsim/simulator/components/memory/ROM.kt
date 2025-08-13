package com.ra4king.circuitsim.simulator.components.memory

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import com.ra4king.circuitsim.simulator.components.memory.ROM.Ports.*

/**
 * @author Roi Atalla
 */
class ROM(
    name: String, override val addressWidth: Int, override val dataWidth: Int,
    override val addressability: Addressability
) :
    Component(name, intArrayOf(addressWidth, 1, dataWidth)), MemoryUnit {

    val addressBits = addressWidth
    val dataBits = dataWidth
    val memory = IntArray(1 shl netAddrBits)

    private val listeners = ArrayList<(Int, Int) -> Unit>()

    init {
        require(!(addressBits > MAX_ADDRESS_BITS || addressBits <= 0)) { "Address bits cannot be more than $MAX_ADDRESS_BITS bits." }
    }

    fun initMemory(memory: IntArray) {
        memory.copyInto(this.memory, 0, 0, minOf(memory.size, this.memory.size))
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

    fun load(address: Int) = memory[effective(address)]

    fun store(address: Int, value: Int) {
        val eff = effective(address)
        memory[eff] = value
        notifyListeners(eff, value)
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

    companion object {
        const val MAX_ADDRESS_BITS = 20
    }
}
