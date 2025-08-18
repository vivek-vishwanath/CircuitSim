package com.ra4king.circuitsim.simulator

import kotlin.math.ceil
import kotlin.math.max

class WireValue(vararg bitList: State) {

    val bits: Array<State> = bitList.toList().toTypedArray()
    val bitSize = bits.size

    enum class State(@JvmField val repr: Char) {
        ONE('1'), ZERO('0'), Z('z');

        fun negate() = when(this) {
            ONE -> ZERO
            ZERO -> ONE
            Z -> Z
        }
    }

    @JvmOverloads
    constructor(bitSize: Int, state: State = State.Z) : this(*Array(bitSize) {state})

    @JvmOverloads
    constructor(value: WireValue, newSize: Int = value.bitSize) :
            this(*Array(newSize) { if (it < value.bitSize) value.getBit(it) else State.ZERO })

    fun merge(value: WireValue): WireValue {
        if (value.bitSize != bitSize)
            throw IllegalArgumentException("Different size wires detected: wanted ${this.bitSize}, found ${value.bitSize}")
        for (i in 0 until bitSize) {
            if (bits[i] == State.Z) bits[i] = value.bits[i]
            else if (value.bits[i] == State.Z) bits[i] = bits[i]
            else if (bits[i] != value.bits[i])
                throw ShortCircuitException(this, value)
        }
        return this
    }

    fun getBit(index: Int) = bits[index]

    fun setBit(index: Int, value: State) {
        bits[index] = value
    }

    fun set(other: WireValue): WireValue {
        require(other.bitSize == bitSize) { "Cannot set wire of different size bits. Wanted: " + bits.size + ", Found: " + other.bits.size }
        System.arraycopy(other.bits, 0, bits, 0, bits.size)
        return this
    }

    fun set(value: Long): WireValue {
        for (i in bitSize - 1 downTo 0)
            bits[i] = if(value and (1L shl i) == 0L) State.ZERO else State.ONE
        return this
    }

    fun setAllBits(state: State) {
        for (i in bitSize - 1 downTo 0) bits[i] = state
    }

    val isValidValue
        get() = bits.isNotEmpty() && bits.all { it != State.Z }

    val value: Int
        get() {
            var value = 0
            for (i in bits.indices) {
                check(bits[i] != State.Z) { "Invalid value" }
                value = value or ((1 shl i) * (if (bits[i] == State.ONE) 1 else 0))
            }
            return value
        }

    val hexString: String
        get() = if (isValidValue) value.toUInt().toString(16).padStart(1 + (bitSize - 1) / 4, '0')
        else "z".repeat(max(0, 1 + (bitSize - 1) / 4))

    val decString: String
        get() = if (isValidValue) value.toUInt().toString().padStart(ceil(bitSize / 3.322).toInt(), '0')
        else "z".repeat(max(0, ceil(bitSize / 3.322).toInt()))

    override fun equals(other: Any?) =
        other is WireValue && other.bitSize == bitSize && (0 ..< bitSize).all { bits[it] == other.bits[it] }

    override fun toString(): String {
        val builder = StringBuilder()
        for (i in this.bitSize - 1 downTo 0) {
            builder.append(bits[i].repr)
        }
        return builder.toString()
    }

    companion object {

        @JvmStatic
        fun of(value: Long, bitSize: Int) = WireValue(bitSize).set(value)
    }

    override fun hashCode() = bits.contentHashCode()
}