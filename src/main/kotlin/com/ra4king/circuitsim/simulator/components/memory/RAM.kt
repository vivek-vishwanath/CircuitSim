package com.ra4king.circuitsim.simulator.components.memory

import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import com.ra4king.circuitsim.simulator.components.memory.RAM.Ports.*

/**
 * @author Roi Atalla
 */
class RAM(name: String, bitSize: Int, addressBits: Int, isSeparateLoadStore: Boolean) :
    Component(name, getPortBits(bitSize, addressBits, isSeparateLoadStore)) {
    
    val addressBits: Int
	val dataBits: Int
	val isSeparateLoadStore: Boolean

    private val noValue: WireValue

    private val listeners = ArrayList<(Int, Int) -> Unit>()

    init {
        require(!(addressBits > 16 || addressBits <= 0)) { "Address bits cannot be more than 16 bits." }

        this.addressBits = addressBits
        this.dataBits = bitSize
        this.isSeparateLoadStore = isSeparateLoadStore

        this.noValue = WireValue(dataBits)
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

    fun store(state: CircuitState, address: Int, data: Int) {
        getMemoryContents(state)[address] = data

        val enabled = state.getLastReceived(getPort(PORT_ENABLE)).getBit(0) != WireValue.State.ZERO
        val load = state.getLastReceived(getPort(PORT_LOAD)).getBit(0) != WireValue.State.ZERO
        val addressValue = state.getLastReceived(getPort(PORT_ADDRESS))
        if (enabled && load && addressValue.isValidValue && addressValue.value == address) {
            state.pushValue(getPort(PORT_DATA), of(data.toLong(), this.dataBits))
        }

        notifyListeners(address, data)
    }

    fun load(circuitState: CircuitState, address: Int) = getMemoryContents(circuitState)[address]

    fun getMemoryContents(circuitState: CircuitState) = circuitState.getComponentProperty(this) as IntArray

    override fun init(circuitState: CircuitState, lastProperty: Any?) {
        circuitState.putComponentProperty(this, IntArray(1 shl addressBits))
    }

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {
        val memory = getMemoryContents(state)

        val enabled = state.getLastReceived(getPort(PORT_ENABLE)).getBit(0) != WireValue.State.ZERO
        val clear = state.getLastReceived(getPort(PORT_CLEAR)).getBit(0) == WireValue.State.ONE
        val load = state.getLastReceived(getPort(PORT_LOAD)).getBit(0) == WireValue.State.ONE
        val store = if (isSeparateLoadStore) state.getLastReceived(getPort(PORT_STORE))
            .getBit(0) == WireValue.State.ONE else !load

        val address = state.getLastReceived(getPort(PORT_ADDRESS))

        when (Ports.entries[portIndex]) {
            PORT_ENABLE, PORT_LOAD -> {
                if (!enabled || !load) {
                    state.pushValue(getPort(PORT_DATA), noValue)
                }
                if (enabled && load && address.isValidValue) {
                    state.pushValue(
                        getPort(PORT_DATA), of(
                            load(state, address.value).toLong(),
                            this.dataBits
                        )
                    )
                }
            }

            PORT_ADDRESS -> if (enabled && load && address.isValidValue) {
                state.pushValue(
                    getPort(PORT_DATA), of(
                        load(state, address.value).toLong(),
                        this.dataBits
                    )
                )
            }

            PORT_CLK -> if (store && value.getBit(0) == WireValue.State.ONE && address.isValidValue) {
                val lastReceived =
                    state.getLastReceived(getPort(if (isSeparateLoadStore) PORT_DATA_IN else PORT_DATA))
                if (lastReceived.isValidValue) {
                    store(state, address.value, lastReceived.value)
                } else {
                    store(state, address.value, of(-1, this.dataBits).value)
                }
            }

            PORT_CLEAR -> if (clear) {
                var i = 0
                while (i < memory.size) {
                    store(state, i, 0)
                    i++
                }
            }
            else -> {}
        }
    }

    enum class Ports {

        PORT_ADDRESS, PORT_ENABLE, PORT_CLK, PORT_LOAD, PORT_CLEAR, PORT_DATA, PORT_DATA_IN, PORT_STORE
    }

    companion object {

        private fun getPortBits(bitSize: Int, addressBits: Int, isSeparateLoadStore: Boolean) =
            if (isSeparateLoadStore) intArrayOf(addressBits, 1, 1, 1, 1, bitSize, bitSize, 1)
            else intArrayOf(addressBits, 1, 1, 1, 1, bitSize)
    }
}
