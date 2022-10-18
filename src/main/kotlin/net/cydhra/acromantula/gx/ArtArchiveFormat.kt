package net.cydhra.acromantula.gx

import net.cydhra.acromantula.features.archives.ArchiveType
import net.cydhra.acromantula.workspace.filesystem.FileEntity

object ArtArchiveType : ArchiveType {
    override val fileTypeIdentifier: String
        get() = "art"

    override fun canAddFile() = false

    override fun onFileAdded(archive: FileEntity, fileEntity: FileEntity) {}

    override fun canMoveFile() = false

    override fun onFileMoved(archive: FileEntity, source: FileEntity, file: FileEntity) {}

    override fun canRenameFile(name: String) = true

    override fun onFileRename(archive: FileEntity, file: FileEntity, newName: String) {}

    override fun canDeleteFile() = false

    override fun onFileDelete(archive: FileEntity, file: FileEntity) {}

    override fun canAddDirectory() = false

    override fun onDirectoryAdded(archive: FileEntity, directory: FileEntity) {}

    override fun canCreateArchiveFromScratch() = false

    override fun createArchiveFromScratch(directory: FileEntity) {}
}