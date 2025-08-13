package com.ra4king.circuitsim.simulator.components.memory

import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.properties.PropertyListValidator

enum class Addressability(val condition: (Int) -> Boolean = { true }) {
    BYTE ({ it == 32 }), HALF_WORD ({ it % 16 == 0 }), WORD;

    val shiftBits = 2 - ordinal
}

interface MemoryUnit {
    val addressWidth: Int
    val dataWidth: Int
    val addressability: Addressability

    val bytesPerEntry get() = when (addressability) {
        Addressability.BYTE -> if (dataWidth == 16) 2 else 4
        Addressability.HALF_WORD -> 2
        Addressability.WORD -> 1
    }

    val addrBitsPerWord get() = when (addressability) {
        Addressability.BYTE -> 2
        Addressability.HALF_WORD -> 1
        Addressability.WORD -> 0
    }

    val netAddrBits get() = addressWidth - addrBitsPerWord

    fun effective(address: Int) = address / when (dataWidth) {
        32 -> when (addressability) {
            Addressability.BYTE -> 4
            Addressability.HALF_WORD -> 2
            Addressability.WORD -> 1
        }
        16 -> when(addressability) {
            Addressability.BYTE -> 2
            Addressability.HALF_WORD -> 2
            Addressability.WORD -> 1
        }
        else -> 1
    }
}

const val ADDRESSABILITY_PROP_NAME = "Address Shift"
val ADDRESSABILITY = Array(32) { i -> Properties.Property(ADDRESSABILITY_PROP_NAME,
    PropertyListValidator(Addressability.entries.filter { it.condition(i+1) })
    { it.shiftBits.toString() }, Addressability.WORD) }