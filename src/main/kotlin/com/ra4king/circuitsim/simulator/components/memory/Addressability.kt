package com.ra4king.circuitsim.simulator.components.memory

import com.ra4king.circuitsim.gui.Properties
import com.ra4king.circuitsim.gui.properties.PropertyListValidator

enum class Addressability(val condition: (Int) -> Boolean = { true }) {
    BYTE ({ it % 16 == 0 }), HALF_WORD ({ it == 32 }), WORD;
}

interface MemoryUnit {
    val addressWidth: Int
    val dataWidth: Int
    val addressability: Addressability

    val bpw get() = when (addressability) {
        Addressability.BYTE -> if (dataWidth == 16) 2 else 1
        Addressability.HALF_WORD -> 2
        Addressability.WORD -> 4
    }

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

val ADDRESSABILITY = Array(32) { i -> Properties.Property("Addressability", PropertyListValidator(Addressability.entries.filter { it.condition(i+1) }), Addressability.WORD) }