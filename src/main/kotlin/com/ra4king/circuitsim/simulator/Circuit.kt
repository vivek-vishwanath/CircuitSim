package com.ra4king.circuitsim.simulator

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer

/**
 * @author Roi Atalla
 */
open class Circuit(var name: String, open val simulator: Simulator) {

	val components = HashSet<Component>()
    private val states = HashSet<CircuitState>()
	val topLevelState = CircuitState.init(this)
    private val listeners = ConcurrentLinkedQueue<CircuitChangeListener>()
    private var exception: RuntimeException? = null

    /**
     * Creates a new Circuit. It is added to the Simulator's list of circuits.
     * @param simulator The Simulator instance this Circuit belongs to.
     */
    init {
        simulator.addCircuit(this)
    }

    private fun <T : Component> add(newComponent: T, oldComponentProps: HashMap<CircuitState, Any?> = HashMap()) {
        newComponent.circuit = this
        components.add(newComponent)
        states.forEach {
            try {
                newComponent.init(it, oldComponentProps[it])
            } catch (e: RuntimeException) {
                if (exception == null) exception = e
            }
        }
        listeners.forEach { it.circuitChanged(this, newComponent, true) }
    }

    private fun <T : Component> remove(component: T, removeLinks: Boolean): HashMap<CircuitState, Any?> {
        states.forEach { it.ensureUnlinked(component, removeLinks) }
        components.remove(component)
        val oldComponentProperties = HashMap<CircuitState, Any?>()
        states.forEach {
            try {
                oldComponentProperties[it] = it.getComponentProperty(component)
                component.uninit(it)
            } catch (exc: RuntimeException) {
                if (exception == null)
                    exception = exc
            }
        }
        component.circuit = null
        listeners.forEach { it.circuitChanged(this, component, false) }
        return oldComponentProperties
    }

    /**
     * Add the Component to this Circuit. Its `init` method is called for each state belonging to this Circuit.
     * All attached listeners are notified of the addition.
     *
     * @param component The Component to add.
     * @return The Component given.
     */
    fun <T : Component> addComponent(component: T): T {
        simulator.runSync {
            if (component.circuit === this)
                return@runSync
            require(component.circuit == null) { "Component already belongs to a circuit." }

            add(component)
        }

        exception?.let { exception = null; throw it }

        return component
    }

    /**
     * Replaces the old Component with the new Component, preserving the component properties. The inBetween Runnable
     * is run in between the remove and the add operations.
     *
     *
     * All attached listeners are notified of the removal before the inBetween is run, then of the addition.
     *
     * @param oldComponent The old Component.
     * @param newComponent The new Component.
     * @param inBetween    Any code that needs to run in between the remove/add operations. May be null.
     */
    fun <T : Component> updateComponent(oldComponent: T, newComponent: T, inBetween: (() -> Any?)?) {
        simulator.runSync {
            val oldComponentProps = remove(oldComponent, false)
            inBetween?.invoke()
            add(newComponent, oldComponentProps)
        }

        exception?.let { exception = null; throw it }
    }

    /**
     * Removes the Component from this Circuit. Its `uninit` method is called for each belonging to this Circuit.
     * All attached listeners are notified of the addition.
     *
     * @param component The Component to remove.
     */
    fun removeComponent(component: Component) {
        simulator.runSync {
            if (!components.contains(component))
                return@runSync
            remove(component, true)
        }

        exception?.let { exception = null; throw it }
    }

    /**
     * Calls `this.removeComponent` on each Component in this Circuit.
     */
    fun clearComponents() = simulator.runSync {
        HashSet(components).forEach { this.removeComponent(it) }
    }

    fun addState(state: CircuitState) {
        states.add(state)
    }

    fun containsState(state: CircuitState?) = states.contains(state)

    fun removeState(state: CircuitState) {
        states.remove(state)
    }

    @JvmSynthetic
    fun forEachState(consumer: (CircuitState) -> Unit) = states.forEach(consumer)

    fun forEachState(consumer: Consumer<CircuitState>) = forEachState { consumer.accept(it) }

    fun addListener(listener: CircuitChangeListener) {
        listeners.add(listener)
    }

    fun interface CircuitChangeListener {
        fun circuitChanged(circuit: Circuit, component: Component?, added: Boolean)
    }

    override fun toString() = "Circuit $name"
}
