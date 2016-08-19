package blast.browser.components

import blast.browser.utils.inSwingThread
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.components.JBTextField
import com.teamdev.jxbrowser.chromium.Browser
import com.teamdev.jxbrowser.chromium.BrowserPreferences
import com.teamdev.jxbrowser.chromium.LoggerProvider
import com.teamdev.jxbrowser.chromium.dom.By
import com.teamdev.jxbrowser.chromium.events.*
import com.teamdev.jxbrowser.chromium.swing.BrowserView
import org.jdom.Element
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.beans.PropertyChangeListener
import java.util.logging.Level
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

abstract class BaseBrowserEditor : UserDataHolderBase(), FileEditor, DataProvider {
    override fun dispose() {
        println("unimplemented dispose")
    }

    override fun getName(): String {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deselectNotify() {
    }

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? = null

    override fun isValid(): Boolean {
        // TODO update
        return true
    }

    override fun isModified(): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun selectNotify() {
        // TODO
    }

    override fun getCurrentLocation(): FileEditorLocation {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addPropertyChangeListener(p0: PropertyChangeListener) {
        println("unplemented addPropertyChangeListener")
    }

    override fun removePropertyChangeListener(p0: PropertyChangeListener) {
        println("unimplemented removePropertyChangeListener")
    }
}
//
//class JfxBrowserEditor(val urlNode: URLVFNode) : BaseBrowserEditor() {
//    private lateinit var browserViewPanel: JFXPanel
//    private lateinit var webView: WebView
//    private lateinit var scene: Scene
//
//
//    override fun getComponent(): JComponent {
//        browserViewPanel = JFXPanel()
//        Platform.runLater {
//            webView = WebView()
//            scene = Scene(webView)
//
//            webView.maxWidthProperty().bind(scene.widthProperty())
//            webView.maxHeightProperty().bind(scene.heightProperty())
//
//            browserViewPanel.scene = scene
//            println(urlNode.targetUrl)
//            webView.engine.load(urlNode.targetUrl.toString())
//        }
//
//        return browserViewPanel
//    }
//
//    override fun getPreferredFocusedComponent(): JComponent {
//        return browserViewPanel
//    }
//}


class JxBrowserEditor(project: Project, urlNode: URLVFNode) : BaseBrowserEditor() {
    private val root = JPanel(GridBagLayout(), true)
    private val textField = JBTextField()
    private val backButton = JButton("<-")
    private val forwardButton = JButton("->")
    private val browser: Browser
    private val browserView: BrowserView
    private val fileManagerEx = FileEditorManagerEx.getInstanceEx(project)

    init {
        browser = JxBrowserManager.initializeJxBrowser()
        browserView = BrowserView(browser)
        createComponent()

        textField.text = urlNode.targetUrl.toString()

        textField.addActionListener { e -> browser.loadURL(e.actionCommand.toString()) }
        backButton.addActionListener { if (browser.canGoBack()) browser.goBack() }
        forwardButton.addActionListener { if (browser.canGoForward()) browser.goForward() }

//        browser.context.networkService.resourceHandler = object : ResourceHandler {
//            override fun canLoadResource(p0: ResourceParams?): Boolean {
//                if (p0!!.resourceType == ResourceType.FAVICON) println(p0!!.url)
//                if(p0!!.resourceType == ResourceType.IMAGE && p0.url.contains("favicon")) println("found favicon url: ${p0.url}")
//                return true
//            }
//        }

//        browser.


        browser.addLoadListener(object : LoadListener {
            override fun onDocumentLoadedInMainFrame(p0: LoadEvent) {
                p0.inSwingThread {
//                    println("BQSDEASDADSADA")
                    val res = it.browser.document.findElements(By.xpath("//link[@rel=\"icon\" or @rel=\"shortcut icon\"]"))
//                    res.forEach {
//                        println("----------")
//                        it.attributes.forEach { println(it.key + " " + it.value) }
//                    }
                    textField.text = it.browser.url
                    urlNode.name = (it.browser.title)
                    fileManagerEx.updateFilePresentation(urlNode)
//                windowManagerEx
                }
            }

            override fun onFailLoadingFrame(p0: FailLoadingEvent?) {
            }

            override fun onStartLoadingFrame(p0: StartLoadingEvent?) {
            }

            override fun onProvisionalLoadingFrame(p0: ProvisionalLoadingEvent?) {

            }

            override fun onFinishLoadingFrame(p0: FinishLoadingEvent?) {
//                println("bang")
            }

            override fun onDocumentLoadedInFrame(frameLoadEvent: FrameLoadEvent) = frameLoadEvent.inSwingThread {
//  val res = it.browser.document.evaluate()
//                res.snapshotNodes.forEach { println(it.nodeName) }

//                println(frameLoadEvent.browser.document.documentElement.innerHTML)
//                val head = frameLoadEvent.browser.document.findElement(By.tagName("head"))
//                println(head.innerHTML)
//                val childZero = head.children[0]
//                val links = childZero.findElement(By.tagName("link"))
//                val children = links.children
////                browserView.i


//                WindowManagerImpl.getInstanceEx()
            }
        })

        browser.loadURL(urlNode.targetUrl.toString())
        IdeFocusManager.getGlobalInstance().requestFocus(browserView,true)
    }

    // when browsers are being restored
    override fun setState(state: FileEditorState) {
        browser.loadURL((state as BrowserEditorState).url)
    }

    // when browsers are being saved
    override fun getState(level: FileEditorStateLevel): FileEditorState {
        // TODO: dont save if the browser has some sort of erro
        return BrowserEditorState(browser.url)
    }

    override fun getData(dataId: String): Any? {
        when (dataId) {
            BlastBrowser.DataKeys.TARGET_TREE.name -> return this
        }
        return null
    }

    private fun createComponent() {
        val c = GridBagConstraints()
        c.fill = GridBagConstraints.HORIZONTAL
        c.weightx = 1.0
        c.gridx = 0
        c.gridy = 0
        root.add(textField, c)

        c.fill = GridBagConstraints.NONE
        c.weightx = 0.0
        c.gridx = 1
        c.gridy = 0
        root.add(backButton, c)

        c.fill = GridBagConstraints.NONE
        c.weightx = 0.0
        c.gridx = 2
        c.gridy = 0
        root.add(forwardButton, c)

        c.fill = GridBagConstraints.BOTH
        c.gridx = 0
        c.gridy = c.gridy + 1
        c.weightx = 1.0
        c.weighty = 1.0
        c.gridwidth = 3
        c.ipady
        root.add(browserView, c)
    }

    override fun getComponent(): JComponent = root
    override fun getPreferredFocusedComponent(): JComponent = browserView
}

class BrowserEditorState(val url: String): FileEditorState {
    override fun canBeMergedWith(otherState: FileEditorState?, level: FileEditorStateLevel?): Boolean {
        return false
    }
}

class BrowserEditorProvider : FileEditorProvider {
    init {
        // todo move to a component initializer
        LoggerProvider.setLevel(Level.OFF);
        BrowserPreferences.setChromiumSwitches("--overscroll-history-navigation=1");
    }

    override fun getEditorTypeId(): String = "blast.browser.editor"
    override fun createEditor(project: Project, vf: VirtualFile): FileEditor {
        JxBrowserManager.ensurePlatformJarDownloaded(project, null)
        return JxBrowserEditor(project, (vf as URLVFNode))
    }

    override fun accept(project: Project, file: VirtualFile): Boolean = file is URLVFNode
    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    override fun readState(sourceElement: Element, project: Project, file: VirtualFile): FileEditorState {
        return BrowserEditorState(sourceElement.getAttribute("url").value)
    }

    override fun writeState(state: FileEditorState, project: Project, targetElement: Element) {
        targetElement.setAttribute("url", (state as BrowserEditorState).url)
    }
}




