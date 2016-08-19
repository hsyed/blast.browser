package blast.browser.components

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileSystem
import java.net.URL
import java.util.*

class BrowserStorageVirtualFilesystem : VirtualFileSystem() {
    override fun getProtocol() = "blast.browser.storagefs"

    override fun findFileByPath(path: String): VirtualFile {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun renameFile(p0: Any?, p1: VirtualFile, p2: String) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createChildFile(p0: Any?, p1: VirtualFile, p2: String): VirtualFile {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun refreshAndFindFileByPath(p0: String): VirtualFile {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun copyFile(p0: Any?, p1: VirtualFile, p2: VirtualFile, p3: String): VirtualFile {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun refresh(p0: Boolean) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteFile(p0: Any?, p1: VirtualFile) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createChildDirectory(p0: Any?, p1: VirtualFile, p2: String): VirtualFile {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addVirtualFileListener(p0: VirtualFileListener) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isReadOnly(): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeVirtualFileListener(p0: VirtualFileListener) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun moveFile(p0: Any?, p1: VirtualFile, p2: VirtualFile) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}


abstract class NonFileVirtualFile(protected val filesystem_: VirtualFileSystem) : VirtualFile() {
    companion object {
        protected val emptyByteArray_ = ByteArray(0)
    }

    override fun contentsToByteArray() = emptyByteArray_
    override fun getLength(): Long = 0

    override fun getInputStream() = throw UnsupportedOperationException()
    override fun getOutputStream(p0: Any?, p1: Long, p2: Long) = throw UnsupportedOperationException()

    override fun isWritable(): Boolean = true
    override fun isValid(): Boolean = true

    override fun refresh(p0: Boolean, p1: Boolean, p2: Runnable?) { }
    override fun getTimeStamp(): Long = 0
    override fun getModificationStamp(): Long =0

    override fun getFileSystem(): VirtualFileSystem = filesystem_
}

abstract class BaseVFNode(protected val parent_: String,
                          filesystem_: VirtualFileSystem,
                          protected val uuid: UUID = UUID.randomUUID()) : NonFileVirtualFile(filesystem_) {
    protected var name_: String? = null

    override fun getName(): String = name_!!
    override fun getParent() = filesystem_.findFileByPath(parent_)

    override fun getPath() = uuid.toString()
}

abstract class BaseVFFileNode(parent_: String,
                              filesystem_: VirtualFileSystem) : BaseVFNode(parent_, filesystem_) {
    override fun isDirectory(): Boolean = false
    override fun getChildren() = emptyArray<VirtualFile>()
}

abstract class BaseVFDirectoryNode(parent_: String,
                                   filesystem_: VirtualFileSystem) : BaseVFNode(parent_, filesystem_) {
    protected var children_: Array<VirtualFile>? = emptyArray()

    override fun isDirectory(): Boolean = true
    override fun getChildren(): Array<out VirtualFile> = children
}

class URLVFNode(val targetUrl: URL, filesystem_: VirtualFileSystem): NonFileVirtualFile(filesystem_) {
    private var name_: String = targetUrl.toString()

    fun setName(name_: String ){
        this.name_ = name_
    }

    override fun getPath(): String {
        return targetUrl.toString()
    }

    override fun isDirectory(): Boolean {
        return false
    }


    override fun getName(): String {
        return name_
    }

    override fun getParent(): VirtualFile? = null

    override fun getChildren(): Array<out VirtualFile> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}