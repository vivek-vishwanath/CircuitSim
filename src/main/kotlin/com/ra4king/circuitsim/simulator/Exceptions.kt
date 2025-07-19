package com.ra4king.circuitsim.simulator


/**
 * @author Roi Atalla
 */
open class SimulationException : RuntimeException {
    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)
}

/**
 * @author Roi Atalla
 */
class OscillationException : SimulationException("Oscillation apparent")


/**
 * @author Roi Atalla
 */
class ShortCircuitException(value1: WireValue, value2: WireValue) :
    SimulationException("Short circuit detected! value1 = $value1, value2 = $value2")
