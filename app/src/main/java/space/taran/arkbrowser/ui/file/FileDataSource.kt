package space.taran.arkbrowser.ui.file

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import space.taran.arkbrowser.utils.toFile
import java.io.*

class FileDataSource(val context: Context) {

    fun getUriForFileByProvider(file: File): Uri {
        return FileProvider.getUriForFile(context,
            "space.taran.arkbrowser.provider",
            file)
    }

    fun getExtSdCards(): List<File> =
        context.getExternalFilesDirs("external")
            .toList()
            .filterNotNull()
            .mapNotNull {
                val path = it.absolutePath
                //todo: improve
                val index = path.lastIndexOf("/Android/data")
                if (index >= 0) {
                    toFile(path.substring(0, index))
                } else {
                    null
                }
            }

    fun getExtSdCardBaseFolder(file: File): File? =
        getExtSdCards().find { file.startsWith(it) }
        //todo fs.normalize `path` before check
}