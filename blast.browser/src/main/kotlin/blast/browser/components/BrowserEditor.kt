package blast.browser.components

import blast.browser.utils.inSwingThread
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBTextField
import com.teamdev.jxbrowser.chromium.Browser
import com.teamdev.jxbrowser.chromium.BrowserPreferences
import com.teamdev.jxbrowser.chromium.LoggerProvider
import com.teamdev.jxbrowser.chromium.events.*
import com.teamdev.jxbrowser.chromium.swing.BrowserView
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.beans.PropertyChangeListener
import java.util.logging.Level
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

abstract class BaseBrowserEditor : UserDataHolderBase(), FileEditor {
    override fun dispose() {
        println("unimplemented dispose")
    }

    override fun getName(): String {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setState(p0: FileEditorState) {
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

class JfxBrowserEditor(val urlNode: URLVFNode) : BaseBrowserEditor() {
    private lateinit var browserViewPanel: JFXPanel
    private lateinit var webView: WebView
    private lateinit var scene: Scene


    override fun getComponent(): JComponent {
        browserViewPanel = JFXPanel()
        Platform.runLater {
            webView = WebView()
            scene = Scene(webView)

            webView.maxWidthProperty().bind(scene.widthProperty())
            webView.maxHeightProperty().bind(scene.heightProperty())

            browserViewPanel.scene = scene
            println(urlNode.targetUrl)
            webView.engine.load(urlNode.targetUrl.toString())
        }

        return browserViewPanel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return browserViewPanel
    }
}


class JxBrowserEditor(urlNode: URLVFNode) : BaseBrowserEditor() {
    private val root = JPanel(GridBagLayout(), true)
    private val textField = JBTextField()
    private val backButton = JButton("<-")
    private val forwardButton = JButton("->")
    private val browser: Browser
    private val browserView: BrowserView

    init {
        browser = JxBrowserManager.initializeJxBrowser()
        browserView = BrowserView(browser)
        createComponent()

        textField.text = urlNode.targetUrl.toString()

        textField.addActionListener { e -> browser.loadURL(e.actionCommand.toString()) }
        backButton.addActionListener { if (browser.canGoBack()) browser.goBack() }
        forwardButton.addActionListener { if (browser.canGoForward()) browser.goForward() }

        browser.addLoadListener(object : LoadListener {
            override fun onDocumentLoadedInMainFrame(p0: LoadEvent?) {
            }

            override fun onFailLoadingFrame(p0: FailLoadingEvent?) {
            }

            override fun onStartLoadingFrame(p0: StartLoadingEvent?) {
            }

            override fun onProvisionalLoadingFrame(p0: ProvisionalLoadingEvent?) {
            }

            override fun onFinishLoadingFrame(p0: FinishLoadingEvent?) {
            }

            override fun onDocumentLoadedInFrame(frameLoadEvent: FrameLoadEvent) = frameLoadEvent.inSwingThread {
                textField.text = it.browser.url
                root.requestFocus()
            }
        })

        browser.loadURL(urlNode.targetUrl.toString())
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
    override fun getPreferredFocusedComponent(): JComponent = root
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
        return JxBrowserEditor((vf as URLVFNode))
    }

    override fun accept(project: Project, file: VirtualFile): Boolean = file is URLVFNode
    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR

}


