package space.taran.arknavigator.mvp.model.repo.preview.generator

import java.nio.file.Path

object ImagePreviewGenerator : PreviewGenerator() {
    override val acceptedExtensions: Set<String> =
        setOf("jpg", "jpeg", "png", "svg", "gif")
    override val acceptedMimeTypes: Set<String>
        get() = setOf("image/jpeg", "image/jpg", "image/png", "image/gif")

    override fun generate(path: Path, previewPath: Path, thumbnailPath: Path) {
        val thumbnail = resizePreviewToThumbnail(path)
        storeThumbnail(thumbnailPath, thumbnail)
    }
}
