package blast.browser.components

import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.ide.favoritesTreeView.actions.DeleteFromFavoritesAction
import com.intellij.ide.util.treeView.AbstractTreeBuilder
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.ex.dummy.DummyFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.pom.Navigatable
import com.intellij.ui.*
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.awt.RelativeRectangle
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.docking.DockContainer
import com.intellij.ui.docking.DockManager
import com.intellij.ui.docking.DockableContent
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.ui.treeStructure.SimpleTreeStructure
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.EditSourceOnEnterKeyHandler
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.util.xmlb.annotations.AbstractCollection
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import java.awt.BorderLayout
import java.awt.Image
import java.net.URL
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.ToolTipManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import kotlin.comparisons.compareValuesBy

interface IDNode : Comparable<BookmarkNode> {
    var displayName: String
    var parent: String?
    val id: String

    override fun compareTo(other: BookmarkNode): Int = compareValuesBy(this, other, { it.id })
}

abstract class BookmarkNode(
        @Attribute override var displayName: String,
        @Attribute override var parent: String?,
        @Attribute override val id: String
) : SimpleNode(), IDNode {
    override fun getName(): String = displayName
}

class BookmarkDirectory(
        displayName: String = "",
        id: String = UUID.randomUUID().toString(),
        parent: String? = "",
        @Tag @AbstractCollection(
                surroundWithTag = false,
                elementTypes = kotlin.arrayOf(blast.browser.components.BookmarkDirectory::class, blast.browser.components.Bookmark::class))
        var nodes: java.util.HashSet<BookmarkNode> = java.util.HashSet<BookmarkNode>()
) : BookmarkNode(displayName, parent, id) {
    override fun isAlwaysLeaf(): Boolean = false
    // TODO don't know why this map was/is needed
    override fun getChildren(): Array<out SimpleNode> = nodes.map { it }.toTypedArray()
}

class Bookmark(
        displayName: String = "",
        @Attribute var url: String = "",
        parent: String? = "",
        id: String = UUID.randomUUID().toString()
) : BookmarkNode(displayName, parent, id) {
    override fun isAlwaysLeaf(): Boolean = true
    override fun getChildren(): Array<out SimpleNode> = emptyArray()
}

interface BookmarkManager {
    companion object {
        fun instance(project: Project): BookmarkManager = ServiceManager.getService(project, BookmarkManagerImpl::class.java)
    }

    val root: BookmarkDirectory get

    fun addNode(parent: BookmarkDirectory, node: BookmarkNode)
    fun removeNode(node: BookmarkNode)
    fun getParent(node: BookmarkNode): BookmarkNode?
}

@State(name = "bookmarks", storages = arrayOf(Storage("bookmark.xml")))
class BookmarkManagerImpl(val project: Project) : PersistentStateComponent<BookmarkDirectory>, BookmarkManager {
    override val root: BookmarkDirectory = BookmarkDirectory("root", "root")
        get
    private var nodes: MutableMap<String, BookmarkNode> = mutableMapOf()

    init {
        root.parent = null
        nodes.put("root", root)
    }

    override fun getParent(node: BookmarkNode): BookmarkNode? = nodes.get(node.id)

    override fun addNode(parent: BookmarkDirectory, node: BookmarkNode) {
        parent.nodes.add(node)
        node.parent = parent.id
        nodes.put(node.id, node)
    }

    override fun removeNode(node: BookmarkNode) {
        (nodes.get(node.id) as BookmarkDirectory).nodes.remove(node)
        node.parent = null
        nodes.remove(node.id)
    }

    override fun getState(): BookmarkDirectory = root

    override fun loadState(state: BookmarkDirectory) {
        root.nodes = state.nodes

        if (state.nodes.isEmpty()) {
            val fixtures = BookmarkDirectory("Programming", "programming", "root")
            addNode(fixtures, Bookmark("Slashdot", "http://www.slashdot.com"))
            addNode(fixtures, Bookmark("Macrumors", "http://www.macrumors.com"))
            addNode(fixtures, Bookmark("Stackoverflow", "http://www.stackoverflow.com"))
            addNode(fixtures, Bookmark("Google", "http://www.google.com"))
            addNode(fixtures, Bookmark("Basecamp", "http://www.basecamp.com"))
            addNode(root, fixtures)
        }

        consume(root, nodes)
    }

    private fun consume(bmd: BookmarkNode, target: MutableMap<String, BookmarkNode>) {
        target.put(bmd.id, bmd)
        when (bmd) { is BookmarkDirectory -> bmd.nodes.map({ consume(it, target) })
        }
    }
}

class BookmarkTreeStructure(val project: Project, val bookmarkManager: BookmarkManager) : SimpleTreeStructure() {
    override fun getRootElement(): Any = bookmarkManager.root
}

class BookmarkTreeBuilder(project: Project,
                          jtree: JTree,
                          treeModel: DefaultTreeModel,
                          treeBase: AbstractTreeStructure) : AbstractTreeBuilder(jtree, treeModel, treeBase, null) {}

class BrowserOpener(bookmark: Bookmark, private val project: Project) : Navigatable {
    private val vf: URLVFNode = URLVFNode(URL(bookmark.url), DummyFileSystem())
    override fun navigate(requestFocus: Boolean) {
        FileEditorManager.getInstance(project).openFile(vf, false)
    }
    override fun canNavigate(): Boolean = true
    override fun canNavigateToSource(): Boolean = true
}


class BookmarkTreeViewPanel(val bookmarkTreeStructure: BookmarkTreeStructure) : JPanel(BorderLayout()), DockContainer, DataProvider {
    private val root = DefaultMutableTreeNode()
    private val treeModel: DefaultTreeModel
    private val myTree: DnDAwareTree
    private val treeBuilder: BookmarkTreeBuilder

    init {
        root.setUserObject(bookmarkTreeStructure.rootElement)
        treeModel = DefaultTreeModel(root)
        myTree = DnDAwareTree(treeModel)

        DockManager.getInstance(bookmarkTreeStructure.project).register(this)

        treeBuilder = BookmarkTreeBuilder(bookmarkTreeStructure.project, myTree, treeModel, bookmarkTreeStructure)

        TreeUtil.installActions(myTree)
        UIUtil.setLineStyleAngled(myTree)
        myTree.setRootVisible(true) // TODO change
        myTree.setShowsRootHandles(true)
        myTree.setLargeModel(true)

        TreeSpeedSearch(myTree)

        ToolTipManager.sharedInstance().registerComponent(myTree)

        myTree.cellRenderer = object : NodeRenderer() {}

        EditSourceOnDoubleClickHandler.install(myTree)
        EditSourceOnEnterKeyHandler.install(myTree)

        val addActionButton = AnActionButton.fromAction(ActionManager.getInstance().getAction("AddNewFavoritesList"))
        addActionButton.templatePresentation.icon = CommonActionsPanel.Buttons.ADD.icon
        addActionButton.shortcut = CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.ADD)

        val editActionButton = AnActionButton.fromAction(ActionManager.getInstance().getAction("EditFavorites"))
        editActionButton.shortcut = CommonShortcuts.CTRL_ENTER

        val deleteActionButton = DeleteFromFavoritesAction()
        deleteActionButton.shortcut = CustomShortcutSet.fromString("DELETE", "BACK_SPACE")

        val decorator = ToolbarDecorator
                .createDecorator(myTree)
                .initPosition()
                .disableAddAction()
                .disableRemoveAction()
                .disableDownAction()
                .disableUpAction()
                .addExtraAction(addActionButton)
                .addExtraAction(editActionButton)
                .addExtraAction(deleteActionButton)

        val action = ActionManager.getInstance().getAction(IdeActions.ACTION_NEW_ELEMENT)
        action.registerCustomShortcutSet(action.shortcutSet, myTree)
        val panel = decorator.createPanel()

        panel.border = IdeBorderFactory.createEmptyBorder()
        add(panel, BorderLayout.CENTER)
        border = IdeBorderFactory.createEmptyBorder()

        treeBuilder.updateFromRoot()
        myTree.repaint()
    }


    override fun add(content: DockableContent<*>, dropTarget: RelativePoint?) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun closeAll() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isDisposeWhenEmpty(): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getContainerComponent(): JComponent = this


    override fun addListener(listener: DockContainer.Listener?, parent: Disposable?) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAcceptAreaFallback(): RelativeRectangle {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun processDropOver(content: DockableContent<*>, point: RelativePoint?): Image {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAcceptArea(): RelativeRectangle {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getContentResponse(content: DockableContent<*>, point: RelativePoint?): DockContainer.ContentResponse {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun resetDropOver(content: DockableContent<*>) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun startDropOver(content: DockableContent<*>, point: RelativePoint?): Image {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun dispose() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hideNotify() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showNotify() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getData(dataId: String?): Any? {
        if (CommonDataKeys.NAVIGATABLE.`is`(dataId)) {
            val res = myTree.selectionPaths
            if (res.size == 1) {
                val tp: TreePath = res[0]
                val uo = (tp.lastPathComponent as DefaultMutableTreeNode).userObject
                when (uo) { is Bookmark -> return BrowserOpener(uo, bookmarkTreeStructure.project)
                }
            }
        }
//        if (CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId)) {
//            val selectedElements = getSelectedElements<Navigatable>(Navigatable::class.java)
//            return if (selectedElements.isEmpty()) null else selectedElements.toTypedArray()
//        }
        return null
    }

}

class BookmarkTreeViewToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val bookmarkManager = BookmarkManager.instance(project)
        val bookmarkTreeStructure = BookmarkTreeStructure(project, bookmarkManager)
        val panel = BookmarkTreeViewPanel(bookmarkTreeStructure)
        val content = ContentFactory.SERVICE.getInstance().createContent(panel, "browser", false)
        toolWindow.contentManager.addContent(content)
    }
}