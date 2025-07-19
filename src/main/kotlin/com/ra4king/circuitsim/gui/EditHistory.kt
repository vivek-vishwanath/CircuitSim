package com.ra4king.circuitsim.gui

import javafx.scene.control.Tab
import java.util.*

/**
 * @author Roi Atalla
 */
class EditHistory(private val circuitSim: CircuitSim) {

    abstract class Edit(val manager: CircuitManager) {
        abstract fun undo()
        abstract fun redo()
    }

    open class CreateCircuit(manager: CircuitManager, val tab: Tab, val idx: Int) : Edit(manager) {
        override fun undo() = manager.simulatorWindow.deleteCircuit(manager, true, true)
        override fun redo() = manager.simulatorWindow.readdCircuit(manager, tab, idx)
    }

    class RenameCircuit(manager: CircuitManager, val tab: Tab, val oldName: String, val newName: String) :
        Edit(manager) {
        override fun undo() = manager.simulatorWindow.renameCircuit(tab, oldName)
        override fun redo() = manager.simulatorWindow.renameCircuit(tab, newName)
    }

    class MoveCircuit(
        manager: CircuitManager,
        val tabs: MutableList<Tab>,
        val tab: Tab,
        val fromIdx: Int,
        val toIdx: Int
    ) : Edit(manager) {
        override fun undo() {
            check(tabs.indexOf(tab) == toIdx) { "Something bad happened!" }
            tabs.removeAt(toIdx)
            tabs.add(fromIdx, tab)
            manager.simulatorWindow.refreshCircuitsTab()
        }

        override fun redo() {
            check(tabs.indexOf(tab) == fromIdx) { "Something bad happened!" }
            tabs.removeAt(fromIdx)
            tabs.add(toIdx, tab)
            manager.simulatorWindow.refreshCircuitsTab()
        }
    }

    class DeleteCircuit(manager: CircuitManager, tab: Tab, idx: Int) : CreateCircuit(manager, tab, idx) {
        override fun undo() = super.redo()
        override fun redo() = super.undo()
    }

    open class AddComponent(manager: CircuitManager, val component: ComponentPeer<*>) : Edit(manager) {
        override fun undo() {
            val set = manager.circuitBoard.components.filter { it == component }.toHashSet()
            manager.mayThrow { manager.circuitBoard.removeElements(set) }
        }

        override fun redo() {
            manager.mayThrow { manager.circuitBoard.addComponent(component) }
        }
    }

    class UpdateComponent(
        manager: CircuitManager,
        val oldComponent: ComponentPeer<*>,
        val newComponent: ComponentPeer<*>
    ) : Edit(manager) {
        override fun undo() {
            manager.mayThrow { manager.circuitBoard.updateComponent(newComponent, oldComponent) }
        }

        override fun redo() {
            manager.mayThrow { manager.circuitBoard.updateComponent(oldComponent, newComponent) }
        }
    }

    class MoveElement(manager: CircuitManager, val element: GuiElement, val dx: Int, val dy: Int) : Edit(manager) {
        override fun undo() {
            element.x -= dx
            element.y -= dy
        }

        override fun redo() {
            element.x += dx
            element.y += dy
        }
    }

    class RemoveComponent(manager: CircuitManager, component: ComponentPeer<*>) : AddComponent(manager, component) {
        override fun undo() = super.redo()
        override fun redo() = super.undo()
    }

    open class AddWire(manager: CircuitManager, val wire: LinkWires.Wire) : Edit(manager) {
        override fun undo() {
            manager.mayThrow { manager.circuitBoard.removeElements(hashSetOf(wire)) }
        }

        override fun redo() {
            manager.mayThrow { manager.circuitBoard.addWire(wire.x, wire.y, wire.length, wire.isHorizontal) }
        }
    }

    class RemoveWire(manager: CircuitManager, wire: LinkWires.Wire) : AddWire(manager, wire) {
        override fun undo() = super.redo()
        override fun redo() = super.undo()
    }

    private val editStack = ArrayDeque<MutableList<Edit>>()
    private val redoStack = ArrayDeque<MutableList<Edit>>()
    private val editListeners = ArrayList<(Edit) -> Unit>()

    fun clear() {
        editStack.clear()
        redoStack.clear()
    }

    private var disableDepth = 0

    fun enable() {
        disableDepth--

        check(disableDepth >= 0) { "This should never happen!" }
    }

    fun disable() {
        disableDepth++
    }

    fun addListener(listener: (Edit) -> Unit) {
        editListeners.add(listener)
    }

    fun removeListener(listener: (Edit) -> Unit) {
        editListeners.remove(listener)
    }

    private var groupDepth = 0
    private val groups = ArrayList<MutableList<Edit>>()

    fun beginGroup() {
        groupDepth++
        this.groups.add(ArrayList())
    }

    fun endGroup() {
        check(groupDepth != 0) { "Mismatched call to endGroup." }
        groupDepth--
        if (groupDepth == 0) {
            check(groups.size == 1) { "There should only be a single group left" }
            val edits = groups[0]
            if (!edits.isEmpty()) {
                editStack.push(edits)
                if (editStack.size > MAX_HISTORY) {
                    editStack.removeLast()
                }
            }
            groups.clear()
        } else {
            groups[groupDepth - 1].addAll(groups[groupDepth])
            groups.removeAt(groupDepth)
        }
    }

    fun clearGroup() {
        checkNotNull(groups) { "No group started" }

        groups[groupDepth - 1].clear()
        groups.subList(groupDepth, groups.size).clear()
    }

    fun addAction(action: Edit) {
        if (disableDepth == 0) {
            beginGroup()
            groups[groupDepth - 1].add(action)
            endGroup()

            redoStack.clear()

            editListeners.forEach { it(action) }
        }
    }

    fun editStackSize() = editStack.size + (if (groups.isEmpty()) 0 else 1)

    fun redoStackSize() = redoStack.size

    fun undo(): CircuitManager? {
        if (editStack.isEmpty()) {
            return null
        }

        val popped = editStack.pop()
        redoStack.push(popped)

        circuitSim.simulator.runSync {
            val circuitManagers = HashSet<CircuitManager>()
            try {
                disable()
                for (i in popped.indices.reversed()) {
                    val edit = popped[i]
                    circuitManagers.add(edit.manager)
                    edit.manager.circuitBoard.rejoinWiresEnabled = false
                    edit.undo()
                    editListeners.forEach { it(edit) }
                }
            } finally {
                enable()
                circuitManagers.forEach { it.circuitBoard.rejoinWiresEnabled = true }
            }
        }

        return popped[0].manager
    }

    fun redo(): CircuitManager? {
        if (redoStack.isEmpty()) return null

        val popped = redoStack.pop()
        editStack.push(popped)
        if (editStack.size > MAX_HISTORY) {
            editStack.removeLast()
        }

        circuitSim.simulator.runSync {
            val circuitManagers = HashSet<CircuitManager>()
            try {
                disable()
                for (edit in popped) {
                    circuitManagers.add(edit.manager)
                    edit.manager.circuitBoard.rejoinWiresEnabled = false
                    edit.redo()
                    editListeners.forEach { it(edit) }
                }
            } finally {
                enable()
                circuitManagers.forEach { it.circuitBoard.rejoinWiresEnabled = true }
            }
        }

        return popped[0].manager
    }

    companion object {
        private const val MAX_HISTORY = 300
    }
}
