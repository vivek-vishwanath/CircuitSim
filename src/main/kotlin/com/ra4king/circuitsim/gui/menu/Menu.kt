import javafx.beans.Observable
import javafx.beans.property.SimpleBooleanProperty
import javafx.event.Event
import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.input.KeyCombination

fun menuBar(init: MenuBarBuilder.() -> Unit): MenuBar {
    return MenuBarBuilder().apply(init).build()
}

class MenuBarBuilder {
    private val menus = mutableListOf<Menu>()

    fun menu(text: String, init: MenuBuilder.() -> Unit) {
        menus += MenuBuilder(text).apply(init).build()
    }

    fun build(): MenuBar = MenuBar().apply {
        this@MenuBarBuilder.menus.forEach { menus.add(it) }
    }
}

class MenuBuilder(private val name: String) {
    private val items = mutableListOf<MenuItem>()

    fun item(
        name: String,
        accelerator: KeyCombination? = null,
        disable: Boolean = false,
        action: (Event) -> Unit
    ): MenuItem {
        val item = MenuItem(name).apply {
            accelerator?.let { this.accelerator = it }
            this.isDisable = disable
            onAction = EventHandler(action)
        }
        items += item
        return item
    }

    fun checkItem(
        name: String,
        accelerator: KeyCombination? = null,
        selected: Boolean? = null,
        binding: SimpleBooleanProperty? = null,
        listener: (Observable, Boolean, Boolean) -> Unit
    ): CheckMenuItem {
        val item = CheckMenuItem(name).apply {
            accelerator?.let { this.accelerator = it }
            selected?.let { isSelected = it }
            binding?.let { selectedProperty().bindBidirectional(it) } ?: this.selectedProperty().addListener(listener)
        }
        items += item
        return item
    }

    fun radioMenu(
        name: String,
        size: Int,
        builder: (Int) -> RadioMenuItem,
        action: (Int) -> Unit
    ): Menu {
        val menu = Menu(name)
        val toggleGroup = ToggleGroup()
        for (i in 0 until size) {
            val item = builder(i).apply {
                this.toggleGroup = toggleGroup
                this.isSelected = i == 0
                setOnAction {
                    action(i)
                }
            }
            menu.items.add(item)
        }
        items += menu
        return menu
    }

    fun separator() {
        items += SeparatorMenuItem()
    }

    fun build(): Menu = Menu(name).apply {
        this@MenuBuilder.items.forEach {
            items.add(it)
        }
    }
}
