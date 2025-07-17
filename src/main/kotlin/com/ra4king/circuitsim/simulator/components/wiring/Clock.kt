package com.ra4king.circuitsim.simulator.components.wiring

import com.ra4king.circuitsim.simulator.*
import com.ra4king.circuitsim.simulator.WireValue.Companion.of
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.Volatile
import kotlin.math.max

/**
 * @author Roi Atalla
 */
class Clock(name: String) : Component(name, intArrayOf(1)) {
    class EnabledInfo(val enabled: Boolean, val hertz: Int) {
        override fun equals(other: Any?) = other is EnabledInfo && enabled == other.enabled && hertz == other.hertz

        override fun hashCode() = Objects.hash(enabled, hertz)
    }

    private class ClockInfo(private val simulator: Simulator) {
        val clocks = ConcurrentHashMap<Clock, Any?>()
        val clockChangeListeners =
            ConcurrentHashMap<(WireValue?) -> Unit, Any?>()

        private class InternalClockInfo(val thread: Thread) {
            val enabled = AtomicBoolean(true)
        }

        private var currentClock: InternalClockInfo? = null
        val clockEnabled = SimpleObjectProperty(EnabledInfo(false, 0))
        var clock = false

        private var lastTickTime: Long = 0
        private var lastPrintTime: Long = 0
        private var tickCount = 0

        @Volatile
        var lastTickCount = 0

        init {
            clockEnabled.addListener(ChangeListener { _, _, newValue ->
                if (newValue.enabled) startClock(newValue.hertz)
                else stopClock(false)
            })
        }

        fun reset() {
            stopClock(true)
            synchronized(this) {
                if (clock) tick()
            }
        }

        @Synchronized
        fun tick() {
            clock = !clock
            val clockValue = of((if (clock) 1 else 0).toLong(), 1)

            simulator.runSync {
                clocks.forEach { (clock: Clock, _: Any?) ->
                    val circuit = clock.circuit
                    circuit?.forEachState { state: CircuitState ->
                        state.pushValue(clock.getPort(PORT), clockValue)
                    }
                }
            }
            clockChangeListeners.forEach { (listener, _) -> listener(clockValue) }
        }

        @Synchronized
        fun startClock(hertz: Int) {
            if (currentClock != null) {
                stopClock(false)
            }

            lastPrintTime = System.nanoTime()
            lastTickTime = lastPrintTime
            tickCount = 0
            lastTickCount = tickCount

            val nanosPerTick = (1e9 / (2L * hertz)).toLong()

            val clockThread = Thread {
                val currentClock = this.currentClock
                if (currentClock == null || Thread.currentThread() != currentClock.thread) {
                    return@Thread
                }
                while (currentClock.enabled.get()) {
                    val now = System.nanoTime()
                    if (now - lastPrintTime >= 1e9) {
                        lastTickCount = tickCount
                        tickCount = 0
                        lastPrintTime = now
                        lastTickTime = now
                    }

                    tick()
                    tickCount++

                    lastTickTime += nanosPerTick

                    val diff = lastTickTime - System.nanoTime()
                    if (diff >= 1e6 || (tickCount shr 1) >= hertz) {
                        try {
                            Thread.sleep(max(1, (diff / 1e6).toLong()))
                        } catch (_: InterruptedException) {
                            break
                        }
                    }
                }
            }

            clockThread.setName("Clock thread")
            clockThread.setDaemon(true)

            currentClock = InternalClockInfo(clockThread)
            clockThread.start()
        }

        fun stopClock(waitForClockToStop: Boolean) {
            if (currentClock != null) {
                val clock: InternalClockInfo? = synchronized(this) {
                    val c = currentClock
                    if (c != null) {
                        currentClock = null
                        c.enabled.set(false)
                    }
                    c
                }

                if (clock != null && waitForClockToStop) {
                    val isClockThread = Thread.currentThread() == clock.thread
                    while (clock.thread.isAlive && !isClockThread) {
                        Thread.yield()
                    }
                }
            }

        }
    }

    override var circuit = super.circuit
        set(value) {
            val old = field
            field = value
            old?.let { simulatorClocks[it.simulator]?.clocks?.remove(this) }
            value?.let { get(it.simulator).clocks[this] = this }
        }

    override fun init(circuitState: CircuitState, lastProperty: Any?) {
        val clock: ClockInfo = get(circuit!!.simulator)
        circuitState.pushValue(getPort(PORT), of((if (clock.clock) 1 else 0).toLong(), 1))
    }

    override fun valueChanged(state: CircuitState, value: WireValue, portIndex: Int) {}

    companion object {
        private val simulatorClocks = ConcurrentHashMap<Simulator, ClockInfo>()

        const val PORT: Int = 0

        private operator fun get(simulator: Simulator) =
            simulatorClocks.computeIfAbsent(simulator) { ClockInfo(it) }

        @JvmStatic
		fun tick(simulator: Simulator) {
            this[simulator].tick()
        }

        @JvmStatic
		fun getTickState(simulator: Simulator) = this[simulator].clock

        @JvmStatic
		fun getLastTickCount(simulator: Simulator) = this[simulator].lastTickCount

        @JvmStatic
		fun reset(simulator: Simulator) {
            this[simulator].reset()
        }

        @JvmStatic
		fun startClock(simulator: Simulator, hertz: Int) {
            this[simulator].clockEnabled.set(EnabledInfo(true, hertz))
        }

        @JvmStatic
		fun isRunning(simulator: Simulator) = this[simulator].clockEnabled.get().enabled

        @JvmStatic
		fun clockEnabledProperty(simulator: Simulator) = this[simulator].clockEnabled

        fun stopClock(simulator: Simulator) {
            this[simulator].clockEnabled.set(EnabledInfo(false, 0))
        }

        @JvmStatic
		fun addChangeListener(simulator: Simulator, listener: (WireValue?) -> Unit) {
            this[simulator].clockChangeListeners[listener] = listener
        }

        fun removeChangeListener(simulator: Simulator, listener: (WireValue?) -> Unit) {
            val clock: ClockInfo = get(simulator)
            clock.clockChangeListeners.remove(listener)
        }
    }
}
