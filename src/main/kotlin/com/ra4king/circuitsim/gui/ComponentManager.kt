package com.ra4king.circuitsim.gui

import com.ra4king.circuitsim.gui.peers.arithmetic.*
import com.ra4king.circuitsim.gui.peers.debugging.BreakpointPeer
import com.ra4king.circuitsim.gui.peers.gates.*
import com.ra4king.circuitsim.gui.peers.io.*
import com.ra4king.circuitsim.gui.peers.memory.*
import com.ra4king.circuitsim.gui.peers.misc.Text
import com.ra4king.circuitsim.gui.peers.plexers.DecoderPeer
import com.ra4king.circuitsim.gui.peers.plexers.DemultiplexerPeer
import com.ra4king.circuitsim.gui.peers.plexers.MultiplexerPeer
import com.ra4king.circuitsim.gui.peers.plexers.PriorityEncoderPeer
import com.ra4king.circuitsim.gui.peers.wiring.*
import com.ra4king.circuitsim.simulator.SimulationException
import javafx.scene.image.Image
import java.lang.reflect.InvocationTargetException

/**
 * @author Roi Atalla
 */
class ComponentManager internal constructor() {
    private val components: MutableList<ComponentLauncherInfo>

    class ComponentLauncherInfo internal constructor(
        val clazz: Class<out ComponentPeer<*>>,
        val name: Pair<String, String>,
        val image: Image?,
        val properties: Properties,
        val showInComponentsList: Boolean,
        val creator: ComponentCreator<*>
    ) {
        override fun hashCode() = clazz.hashCode() xor name.hashCode() xor
                (image?.hashCode() ?: 0) xor properties.hashCode() xor creator.hashCode()

        override fun equals(other: Any?) = other is ComponentLauncherInfo && clazz == other.clazz && name == other.name
    }

    fun interface ComponentManagerInterface {
        
        fun addComponent(
            name: Pair<String, String>,
            image: Image,
            defaultProperties: Properties,
            showInComponentsList: Boolean
        )
    }

    init {
        components = ArrayList<ComponentLauncherInfo>()
        registerDefaultComponents()
    }

    fun get(name: Pair<String, String>) = components.find { it.name == name }
        ?: throw IllegalArgumentException("Component not registered: $name")

    @Throws(SimulationException::class)  // To be able to use getClass(), ComponentPeer cannot be parameterized.
    fun get(clazz: Class<out ComponentPeer<*>>, properties: Properties): ComponentLauncherInfo {
        var firstComponent: ComponentLauncherInfo? = null

        for (component in components) {
            if (component.clazz == clazz) {
                firstComponent = component

                if (properties.intersect(component.properties) == component.properties) {
                    return component
                }
            }
        }
        
        return firstComponent ?: throw SimulationException("Component not registered: $clazz")
    }

    fun forEach(consumer: (ComponentLauncherInfo) -> Unit) {
        components.forEach(consumer)
    }

    fun <T : ComponentPeer<*>> register(clazz: Class<T>) {
        try {
            val creator = forClass(clazz)

            val method = clazz.getMethod("installComponent", ComponentManagerInterface::class.java)
            method.invoke(
                null,
                ComponentManagerInterface { name: Pair<String, String>, image: Image, defaultProperties: Properties, showInComponentsList: Boolean ->
                    val info =
                        ComponentLauncherInfo(clazz, name, image, defaultProperties, showInComponentsList, creator)
                    if (!components.contains(info)) {
                        components.add(info)
                    }
                })
        } catch (_: NoSuchMethodException) {
            throw RuntimeException("Must implement: public static void installComponent(ComponentManagerInterface): $clazz")
        } catch (exc: RuntimeException) {
            throw exc
        } catch (exc: Exception) {
            throw RuntimeException(exc)
        }
    }

    private fun registerDefaultComponents() {
        register(PinPeer::class.java)
        register(ConstantPeer::class.java)
        register(Probe::class.java)
        register(ClockPeer::class.java)
        register(SplitterPeer::class.java)
        register(Tunnel::class.java)
        register(SimpleTransistorPeer::class.java)
        register(PowerPeer::class.java)
        register(GroundPeer::class.java)
        register(TransistorPeer::class.java)

        register(AndGatePeer::class.java)
        register(NandGatePeer::class.java)
        register(OrGatePeer::class.java)
        register(NorGatePeer::class.java)
        register(XorGatePeer::class.java)
        register(XnorGatePeer::class.java)
        register(NotGatePeer::class.java)
        register(ControlledBufferPeer::class.java)

        register(MultiplexerPeer::class.java)
        register(DemultiplexerPeer::class.java)
        register(DecoderPeer::class.java)
        register(PriorityEncoderPeer::class.java)

        register(RegisterPeer::class.java)
        register(SRFlipFlopPeer::class.java)
        register(DFlipFlopPeer::class.java)
        register(RAMPeer::class.java)
        register(ROMPeer::class.java)

        register(AdderPeer::class.java)
        register(SubtractorPeer::class.java)
        register(MultiplierPeer::class.java)
        register(DividerPeer::class.java)
        register(NegatorPeer::class.java)
        register(ComparatorPeer::class.java)
        register(BitExtenderPeer::class.java)
        register(ShifterPeer::class.java)
        register(RandomGeneratorPeer::class.java)

        register(Button::class.java)
        register(LED::class.java)
        register(LEDMatrix::class.java)
        register(HexDisplay::class.java)
        register(SevenSegmentDisplay::class.java)

        register(Text::class.java)

        register(BreakpointPeer::class.java)
    }

    fun interface ComponentCreator<T : ComponentPeer<*>> {
        fun createComponent(properties: Properties?, x: Int, y: Int): T
    }

    companion object {
        fun <T : ComponentPeer<*>> forClass(clazz: Class<T>): ComponentCreator<T> {
            return ComponentCreator { properties: Properties?, x: Int, y: Int ->
                try {
                    return@ComponentCreator clazz.getConstructor(Properties::class.java, Integer.TYPE, Integer.TYPE)
                        .newInstance(properties, x, y)
                } catch (_: NoSuchMethodException) {
                    throw RuntimeException("Must have constructor taking (Properties props, int x, int y)")
                } catch (exc: InvocationTargetException) {
                    if (exc.targetException is SimulationException) {
                        throw exc.targetException
                    }

                    throw RuntimeException(exc.targetException)
                } catch (exc: RuntimeException) {
                    throw exc
                } catch (exc: Exception) {
                    throw RuntimeException(exc)
                }
            }
        }
    }
}
