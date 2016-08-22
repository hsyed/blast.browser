package blast.browser.components

import blast.browser.utils.actionButton
import com.intellij.icons.AllIcons
import com.intellij.ide.FileIconProvider
import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.ide.util.treeView.AbstractTreeBuilder
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.pom.Navigatable
import com.intellij.ui.CommonActionsPanel
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.awt.RelativeRectangle
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.docking.DockContainer
import com.intellij.ui.docking.DockManager
import com.intellij.ui.docking.DockableContent
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.EditSourceOnEnterKeyHandler
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.Image
import java.net.URL
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class BookmarkTreeBuilder(jtree: JTree,
                          treeModel: DefaultTreeModel,
                          treeBase: AbstractTreeStructure) : AbstractTreeBuilder(jtree, treeModel, treeBase, null) {
    init {

    }
}

class BrowserOpener(private val url: String, private val project: Project) : Navigatable {
    override fun navigate(requestFocus: Boolean) {
        val manager = FileEditorManager.getInstance(project)
        val vf: URLVFNode = URLVFNode(URL(url), BrowserStorageVirtualFilesystem.instance)
        manager.openFile(vf, true)
    }

    override fun canNavigate(): Boolean = true
    override fun canNavigateToSource(): Boolean = true
}

interface BookmarkTreeModel {
    val tree: Tree
    val treeBuilder: BookmarkTreeBuilder
}

class BookmarkTreeViewPanel(
        val project: Project,
        val bookmarkManager: BookmarkManagerImpl
) : JPanel(BorderLayout()), DockContainer, DataProvider, BookmarkTreeModel, DataContext {
    private val root = DefaultMutableTreeNode()
    private val treeModel: DefaultTreeModel

    override val tree: DnDAwareTree
    override val treeBuilder: BookmarkTreeBuilder


    init {
        root.setUserObject(bookmarkManager.rootElement)
        treeModel = DefaultTreeModel(root)
        tree = DnDAwareTree(treeModel)

        DockManager.getInstance(project).register(this)

        treeBuilder = BookmarkTreeBuilder(tree, treeModel, bookmarkManager)


        TreeUtil.installActions(tree)
        UIUtil.setLineStyleAngled(tree)
        tree.isRootVisible = true // TODO change
        tree.showsRootHandles = true
        tree.isLargeModel = true

        TreeSpeedSearch(tree)

        ToolTipManager.sharedInstance().registerComponent(tree)

        tree.cellRenderer = object : NodeRenderer() {}

        EditSourceOnDoubleClickHandler.install(tree)
        EditSourceOnEnterKeyHandler.install(tree)

        val decorator = ToolbarDecorator
                .createDecorator(tree)
                .initPosition()
                .disableAddAction()
                .disableRemoveAction()
                .disableDownAction()
                .disableUpAction()
                .addExtraActions(
                        actionButton<AddNewBrowserBookmarkGroupActionButton>(
                                icon = CommonActionsPanel.Buttons.ADD.icon,
                                shortcut = CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.ADD),
                                contextComponent = this),
                        actionButton<EditBookmarkNodeEntryAction>(
                                icon = CommonActionsPanel.Buttons.EDIT.icon,
                                shortcut = CommonShortcuts.CTRL_ENTER,
                                contextComponent = this),
                        actionButton<DeleteSelectedBookmarkAction>(
                                icon = CommonActionsPanel.Buttons.REMOVE.icon,
                                shortcut = CustomShortcutSet.fromString("DELETE", "BACK_SPACE"),
                                contextComponent = this)
                )


        val action = ActionManager.getInstance().getAction(IdeActions.ACTION_NEW_ELEMENT)
        action.registerCustomShortcutSet(action.shortcutSet, tree)
        val panel = decorator.createPanel()

        panel.border = IdeBorderFactory.createEmptyBorder()
        add(panel, BorderLayout.CENTER)
        border = IdeBorderFactory.createEmptyBorder()

        bookmarkManager.addBookmarkListener(object : BookmarkListener {
            override fun itemUpdated(node: BookmarkNode) {
                treeBuilder.queueUpdateFrom(node, false)
                tree.repaint()
            }

            override fun parentChanged(parent: BookmarkDirectory) {
                treeBuilder.queueUpdateFrom(parent, false)
                tree.repaint()
            }

            override fun rootsChanged() {
                treeBuilder.queueUpdateFrom(bookmarkManager.root, false)
                tree.repaint()
            }

            override fun itemAdded(parent: BookmarkDirectory, node: BookmarkNode) {
            }

            override fun itemRemoved(parent: BookmarkDirectory, node: BookmarkNode) {
                treeBuilder.select(parent)
            }
        })


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

    }

    override fun hideNotify() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showNotify() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getData(dataId: String): Any? {
        when (dataId) {
            CommonDataKeys.NAVIGATABLE.name -> {
                val res = tree.selectionPaths
                if (res.size == 1) {
                    val tp: TreePath = res[0]
                    val uo = (tp.lastPathComponent as DefaultMutableTreeNode).userObject
                    when (uo) { is Bookmark -> return BrowserOpener(uo.url, project)
                    }
                }
            }
            BlastBrowser.DataKeys.TARGET_TREE.name -> return tree

            else -> return null
        }
        return null

//        if (CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId)) {
//            val selectedElements = getSelectedElements<Navigatable>(Navigatable::class.java)
//            return if (selectedElements.isEmpty()) null else selectedElements.toTypedArray()
//        }
    }
}

class BookmarkTreeViewToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val bookmarkManager = ServiceManager.getService(project, BookmarkManagerImpl::class.java)
        val panel = BookmarkTreeViewPanel(project, bookmarkManager)
        val content = ContentFactory.SERVICE.getInstance().createContent(panel, "browser", false)
        toolWindow.contentManager.addContent(content)
    }
}

class Monkey: FileIconProvider {
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
        if ((file as? URLVFNode) != null) return AllIcons.General.Web
        else return null
    }
}





