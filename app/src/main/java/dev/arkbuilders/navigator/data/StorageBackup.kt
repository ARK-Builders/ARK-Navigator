package dev.arkbuilders.navigator.data

import dev.arkbuilders.arkfilepicker.folders.FoldersRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dev.arkbuilders.arklib.ArkFiles
import dev.arkbuilders.arklib.arkFolder
import dev.arkbuilders.navigator.data.preferences.PreferenceKey
import dev.arkbuilders.navigator.data.preferences.Preferences
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.outputStream

class StorageBackup @Inject constructor(
    private val foldersRepo: FoldersRepo,
    private val preferences: Preferences
) {
    private val keepDates: List<String> = getKeepBackupDates()
    private val today: String = keepDates[0]

    private val foldersToIgnore = listOf(
        ArkFiles.PREVIEWS_STORAGE_FOLDER.name,
        ArkFiles.THUMBNAILS_STORAGE_FOLDER.name
    )
    private val filesToIgnore = listOf<String>()

    fun backup() = CoroutineScope(Dispatchers.IO).launch {
        if (!preferences.get(PreferenceKey.BackupEnabled))
            return@launch

        val allRoots = foldersRepo.provideFolders().keys

        allRoots
            .filter { !isTodayBackupExists(it) }
            .forEach { backupRoot(it) }

        allRoots.forEach { deleteOldBackups(it) }
    }

    private fun backupRoot(root: Path) {
        val backupPath = root
            .resolve(BACKUP_FOLDER)
            .createDirectories()
            .resolve("$BACKUP_NAME_PREFIX$today.zip")

        val arkPath = root.arkFolder()

        val filesToBackup = arkPath.toFile()
            .walkTopDown()
            .onEnter { enteredFolder -> enteredFolder.name !in foldersToIgnore }
            .toList()
            .map { it.toPath() }
            // only files are needed, folders will be created automatically in zip
            .filter { file ->
                file.isRegularFile() &&
                    file.fileName.toString() !in filesToIgnore
            }

        if (filesToBackup.isEmpty()) return

        ZipOutputStream(backupPath.outputStream()).use { zipOut ->
            filesToBackup
                .forEach { backupFile ->
                    val name = arkPath.relativize(backupFile).toString()
                    val zipEntry = ZipEntry(name)
                    zipOut.putNextEntry(zipEntry)
                    backupFile.inputStream().use {
                        it.copyTo(zipOut)
                    }
                }
        }
    }

    private fun deleteOldBackups(root: Path) {
        val backupFolder = root.resolve(BACKUP_FOLDER)
        if (backupFolder.notExists()) return
        val keepBackups = keepDates.map { date ->
            backupFolder.resolve("$BACKUP_NAME_PREFIX$date.zip")
        }
        backupFolder.listDirectoryEntries().forEach { backup ->
            if (!keepBackups.contains(backup))
                backup.deleteIfExists()
        }
    }

    private fun isTodayBackupExists(root: Path): Boolean {
        val todayBackup = root
            .resolve(BACKUP_FOLDER)
            .resolve("$BACKUP_NAME_PREFIX$today.zip")
        return todayBackup.exists()
    }

    private fun getKeepBackupDates(): List<String> {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val keepDates = mutableListOf<OffsetDateTime>()
        keepDates.add(now)
        var tmpDate = now.minusDays(1)
        repeat(KEEP_BACKUPS_COUNT - 1) {
            keepDates.add(tmpDate)
            tmpDate = tmpDate.minusDays(1)
        }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return keepDates.map { formatter.format(it) }
    }

    companion object {
        private val BACKUP_FOLDER = Path(".ark-backups")
        private val BACKUP_NAME_PREFIX = Path("backup-")
        private const val KEEP_BACKUPS_COUNT = 7
    }
}
