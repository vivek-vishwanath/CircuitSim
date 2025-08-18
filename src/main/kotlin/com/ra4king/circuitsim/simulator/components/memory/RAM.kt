package com.ra4king.circuitsim.simulator.components.memory

import com.ra4king.circuitsim.gui.properties.PropertyMemoryValidator
import com.ra4king.circuitsim.simulator.CircuitState
import com.ra4king.circuitsim.simulator.Component
import com.ra4king.circuitsim.simulator.WireValue
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import com.ra4king.circuitsim.simulator.components.memory.RAM.Ports.*
import java.io.File

/**
 * @author Roi Atalla
 */
class RAM(
    name: String, override val addressWidth: Int,
    override val dataWidth: Int,
    override val addressability: Addressability, val isSeparateLoadStore: Boolean, val srcFile: File?
) :
    Component(name, getPortBits(dataWidth, addressWidth, isSeparateLoadStore)),
    MemoryUnit {

    private val addressBits = addressWidth
    val dataBits = dataWidth

    private val noValue: WireValue

    private val listeners = ArrayList<(Int, Int) -> Unit>()

    init {
        require(!(addressBits > MAX_ADDRESS_BITS || addressBits <= 0)) { "Address bits cannot be more than $MAX_ADDRESS_BITS bits." }

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
        val array = getMemoryContents(state)
        array[effective(address)] = data
        state.putComponentProperty(this, array)

        val enabled = state.getLastReceived(getPort(PORT_ENABLE)).getBit(0) != WireValue.State.ZERO
        val load = state.getLastReceived(getPort(PORT_LOAD)).getBit(0) != WireValue.State.ZERO
        val addressValue = state.getLastReceived(getPort(PORT_ADDRESS))
        if (enabled && load && addressValue.isValidValue && addressValue.value == address) {
            state.pushValue(getPort(PORT_DATA), of(data.toLong(), this.dataBits))
        }

        notifyListeners(effective(address), data)
    }

    fun load(circuitState: CircuitState, address: Int) = getMemoryContents(circuitState)[effective(address)]

    fun getMemoryContents(circuitState: CircuitState) = circuitState.getComponentProperty(this) as? IntArray ?: IntArray(1 shl netAddrBits)

    override fun init(circuitState: CircuitState, lastProperty: Any?) {
        val contents = srcFile?.let { try { PropertyMemoryValidator.parseFile(it, netAddrBits, dataBits) } catch (_: Exception) { null } }
        val memory = (lastProperty as? IntArray)?.let { prev -> IntArray(1 shl netAddrBits) { if (it < prev.size) prev[it] else 0 } }
            ?: contents?.let { IntArray(1 shl netAddrBits) { i -> contents[i / 16]?.values?.get(i % 16)?.value?.toUInt(16)?.toInt() ?: 0 } }
        circuitState.putComponentProperty(this, memory)
    }

    override fun uninit(circuitState: CircuitState) {
        circuitState.removeComponentProperty(this)
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

        const val MAX_ADDRESS_BITS = 20

        private fun getPortBits(bitSize: Int, addressBits: Int, isSeparateLoadStore: Boolean) =
            if (isSeparateLoadStore) intArrayOf(addressBits, 1, 1, 1, 1, bitSize, bitSize, 1)
            else intArrayOf(addressBits, 1, 1, 1, 1, bitSize)
    }
}
