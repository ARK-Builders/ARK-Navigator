package space.taran.arknavigator.mvp.model.repo.preview.generator

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.nio.file.Path
import java.util.zip.ZipFile

object LinkPreviewGenerator {
    private const val IMAGE_FILE = "link.png"

    fun generate(source: Path): Bitmap {
        val zip = ZipFile(source.toFile())
        val entries = zip.entries()
        val imageEntry = entries
            .asSequence()
            .find { entry -> entry.name == IMAGE_FILE }
            ?: error("No image inside .link file")

        return BitmapFactory.decodeStream(zip.getInputStream(imageEntry))
    }
}
