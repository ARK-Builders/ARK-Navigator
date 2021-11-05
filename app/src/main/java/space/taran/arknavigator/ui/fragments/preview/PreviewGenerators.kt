package space.taran.arknavigator.ui.fragments.preview

import android.graphics.Bitmap
import space.taran.arknavigator.ui.fragments.preview.generator.PdfPreviewGenerator
import java.nio.file.Path

object PreviewGenerators {
    var BY_EXT: Map<String, (Path) -> Bitmap> = mapOf(
        "pdf" to { path: Path -> PdfPreviewGenerator.generate(path) }
        //todo moar
    )
}