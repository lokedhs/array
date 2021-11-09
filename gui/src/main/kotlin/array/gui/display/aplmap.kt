package array.gui.display

import array.APLMap
import array.APLValue
import array.FormatStyle
import com.sun.javafx.collections.ImmutableObservableList
import javafx.beans.property.ListProperty
import javafx.beans.property.ListPropertyBase
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Region
import javafx.util.Callback

sealed class APLValueTreeNode {
    class RootNode
    class EntryNode(val key: APLValue.APLValueKey, val value: APLValue) : APLValueTreeNode()
    class ChildNode(val value: APLValue) : APLValueTreeNode()
}

class APLMapRenderer(override val value: APLMap) : ValueRenderer {
    private val entries: List<TreeItem<APLValueTreeNode>> = value.content.entries
        .sortedWith { a, b -> a.key.value.compare(b.key.value) }
        .map { EntryTreeItem(APLValueTreeNode.EntryNode(it.key, it.value)) }

    override val text = value.formatted(FormatStyle.PLAIN)

    override fun renderValue(): Region {
        val tree = TreeView<APLValueTreeNode>()
        tree.cellFactory = Callback<TreeView<APLValueTreeNode>, TreeCell<APLValueTreeNode>> {
            APLMapCellImpl()
        }
        val rootList = TreeItem<APLValueTreeNode>()
        rootList.isExpanded = true
        rootList.children.addAll(entries)
        tree.root = rootList
        return tree
    }
}

class APLMapCellImpl : TreeCell<APLValueTreeNode>() {
    val wrapper: BorderPane

    init {
        wrapper = BorderPane()
        children.add(wrapper)
    }

    var i = 0

    override fun updateItem(item: APLValueTreeNode?, empty: Boolean) {
        if(item == null) {
            wrapper.center = null
        } else {
            wrapper.center = Label("Instance: ${i++}")
        }
    }
}

class EntryTreeItem(item: APLValueTreeNode.EntryNode) : TreeItem<APLValueTreeNode>(item) {
    //private val children = SimpleListProperty<TreeItem<APLValueTreeNode>>().apply { add(ChildTreeItem(APLValueTreeNode.ChildNode(item.value))) }

    init {
        children.add(ChildTreeItem(APLValueTreeNode.ChildNode(item.value)))
    }

    override fun isLeaf() = false
//    override fun getChildren(): ObservableList<TreeItem<APLValueTreeNode>> = children
}

class ChildTreeItem(item: APLValueTreeNode.ChildNode) : TreeItem<APLValueTreeNode>(item) {
    override fun isLeaf() = true
}
