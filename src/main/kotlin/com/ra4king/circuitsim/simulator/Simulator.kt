package com.ra4king.circuitsim.simulator

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

/**
 * @author Roi Atalla
 */
open class Simulator {

    val circuits = HashSet<Circuit>()
    var linksToUpdate = LinkedHashSet<Pair<CircuitState, Port.Link>>()
        private set
    private var temp = LinkedHashSet<Pair<CircuitState, Port.Link>>()
    private val lastShortCircuitedLinks = HashSet<Pair<CircuitState, Port.Link>>()
    private val history = HashSet<MutableCollection<Pair<CircuitState, Port.Link>>>()

    // Create a Lock with a fair policy
    val lock = ReentrantLock(true)

    /**
     * Allows execution of code that is synchronized with the Simulator
     *
     *
     * Similar to but more efficient than `synchronized(simulator) { runnable.run(); }`
     *
     * @param runnable The block of code to run synchronously
     */
    fun <T> runSync(runnable: () -> T): T {
        lock.lock()
        return try {
            runnable()
        } finally {
            lock.unlock()
        }
    }

    fun hasLinksToUpdate(): Boolean {
        val tmp = AtomicBoolean()
        runSync { tmp.set(!linksToUpdate.isEmpty()) }
        return tmp.get()
    }

    /**
     * Clears all circuits and queue of un-propagated links.
     */
    fun clear() {
        runSync {
            circuits.clear()
            linksToUpdate.clear()
            temp.clear()
            lastShortCircuitedLinks.clear()
            history.clear()
        }
    }

    /**
     * Resets all CircuitStates of all attached Circuits.
     */
    fun reset() {
        runSync { circuits.forEach {
            circuit -> circuit.forEachState {
                it.reset()
            } } }
    }

    /**
     * Add the Circuit to this Simulator.
     *
     * @param circuit The Circuit to be added.
     */
    fun addCircuit(circuit: Circuit) {
        runSync { circuits.add(circuit) }
    }

    /**
     * Remove the Circuit from this Simulator.
     *
     * @param circuit The Circuit to be removed.
     */
    fun removeCircuit(circuit: Circuit) {
        runSync { circuits.remove(circuit) }
    }

    /**
     * Notify the Simulator the specified port has pushed a new value.
     *
     * @param state The CircuitState in which the Port has pushed the new value.
     * @param port  The Port that pushed the new value.
     */
    fun valueChanged(state: CircuitState, port: Port) {
        valueChanged(state, port.link)
    }

    /**
     * Notify the Simulator the specified Link has received new values.
     *
     * @param state The CircuitState in which the Link has received new values.
     * @param link  The Link that has received new values.
     */
    fun valueChanged(state: CircuitState, link: Port.Link) {
        runSync { linksToUpdate.add(Pair(state, link)) }
    }

    /**
     * Removes the Link from the processing queue.
     */
    fun linkRemoved(link: Port.Link) {
        runSync {
            linksToUpdate
                .filter { it.second == link }
                .forEach { linksToUpdate.remove(it) }
        }
    }

    private val stepping = AtomicBoolean(false)

    /**
     * Perform only a single propagation step. This is thread-safe.
     */
    fun step() {
        runSync {
            if (stepping.get()) {
                return@runSync
            }
            try {
                stepping.set(true)

                val tmp = linksToUpdate
                linksToUpdate = temp
                linksToUpdate.clear()
                temp = tmp

                var lastException: RuntimeException? = null

                for (pair in temp) {
                    val (state, link) = pair

                    // The Link or CircuitState may have been removed
                    if (link.circuit == null || !state.circuit.containsState(state))
                        continue

                    try {
                        state.propagateSignal(link)
                    } catch (_: ShortCircuitException) {
                        lastShortCircuitedLinks.add(pair)
                    } catch (exc: RuntimeException) {
                        exc.printStackTrace()
                        lastException = exc
                    }
                }

                if (lastException != null) throw lastException

                // Only throw the ShortCircuitException if there's no more links to update, which means that links have
                // reached a steady state
                if (!lastShortCircuitedLinks.isEmpty() && linksToUpdate.isEmpty()) {
                    for (pair in lastShortCircuitedLinks) {
                        // Check if the link is still valid and if there's a short circuit
                        if (pair.second.circuit != null && pair.first.isShortCircuited(pair.second)) {
                            // Cause a ShortCircuitException to be thrown
                            pair.first.getMergedValue(pair.second)
                        }
                    }


                    // No exception was thrown, so there's no more short circuits
                    lastShortCircuitedLinks.clear()
                }
            } finally {
                stepping.set(false)
            }
        }
    }

    /**
     * Continuously steps the simulation until no more propagation is needed. This is thread-safe.
     */
    fun stepAll() {
        runSync {
            if (stepping.get()) {
                return@runSync
            }
            history.clear()

            var repeatCount = 0

            var lastException: RuntimeException? = null
            var lastShortCircuit: ShortCircuitException? = null

            while (!linksToUpdate.isEmpty()) {
                if (history.contains(linksToUpdate)) {
                    if (++repeatCount == 10) // since short circuits are retried, it looks like they're oscillating
                        throw OscillationException()
                }

                history.add(LinkedHashSet(linksToUpdate))

                try {
                    step()
                } catch (exc: ShortCircuitException) {
                    // ignore until all updates are done
                    lastShortCircuit = exc
                } catch (exc: RuntimeException) {
                    // ignore until all updates are done
                    lastException = exc
                }
            }

            if (lastException != null) throw lastException
            if (lastShortCircuit != null) throw lastShortCircuit
        }
    }
}
