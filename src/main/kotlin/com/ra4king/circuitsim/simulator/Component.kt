package com.ra4king.circuitsim.simulator

/**
 * @author Roi Atalla
 */
abstract class Component protected constructor(var name: String, portBits: IntArray) {
    open var circuit: Circuit? = null
    private val ports = Array(portBits.size) {Port(this, it, portBits[it])}

    fun getPort(portIndex: Int) = ports[portIndex]

    fun <T : Enum<T>> getPort(portIndex: Enum<T>) = ports[portIndex.ordinal]

    val numPorts = ports.size

    open fun init(circuitState: CircuitState, lastProperty: Any?) {}

    open fun uninit(circuitState: CircuitState) {}

    abstract fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int)

    override fun toString() = name.ifEmpty { "${javaClass.getName()}@${Integer.toHexString(hashCode())}" }
}
