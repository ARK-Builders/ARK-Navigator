package space.taran.arknavigator.mvp.model.repo.preview

import android.util.Log
import space.taran.arknavigator.mvp.model.repo.extra.ImageMetaExtra
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.PREVIEWS
import space.taran.arknavigator.utils.extension
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class PreviewAndThumbnail(val preview: Path, val thumbnail: Path) {

    companion object {

        private val PREVIEWS_STORAGE: Path =
            Paths.get("${App.instance.cacheDir}/previews")
        private val THUMBNAILS_STORAGE: Path =
            Paths.get("${App.instance.cacheDir}/thumbnails")

        private fun previewPath(id: ResourceId): Path =
            PREVIEWS_STORAGE.resolve(id.toString())
        private fun thumbnailPath(id: ResourceId): Path =
            THUMBNAILS_STORAGE.resolve(id.toString())

        init {
            Files.createDirectories(PREVIEWS_STORAGE)
            Files.createDirectories(THUMBNAILS_STORAGE)
        }

        fun locate(path: Path, resource: ResourceMeta): PreviewAndThumbnail? {
            val thumbnail = thumbnailPath(resource.id)
            if (!Files.exists(thumbnail)) {
                // means that we couldn't generate anything for this kind of resource
                return null
            }

            if (ImageMetaExtra.ACCEPTED_EXTENSIONS.contains(extension(path))) {
                return PreviewAndThumbnail(
                    preview = path, // using the resource itself as its preview
                    thumbnail = thumbnail
                )
            }

            return PreviewAndThumbnail(
                preview = previewPath(resource.id),
                thumbnail = thumbnail
            )
        }

        fun forget(id: ResourceId) {
            Files.deleteIfExists(previewPath(id))
            Files.deleteIfExists(thumbnailPath(id))
        }

        fun generate(path: Path, meta: ResourceMeta) {
            if (Files.isDirectory(path)) {
                throw AssertionError("Previews for folders are constant")
            }

            val previewPath = previewPath(meta.id)
            val thumbnailPath = thumbnailPath(meta.id)

            if (!previewExists(previewPath, thumbnailPath)) {
                Log.d(PREVIEWS, "Generating preview for ${meta.id} ($path)")
                PreviewGenerators.generate(path, previewPath, thumbnailPath)
            }
        }

        private fun previewExists(previewPath: Path, thumbnailPath: Path): Boolean {
            if (Files.exists(previewPath)) {
                if (!Files.exists(thumbnailPath)) {
                    throw AssertionError("Thumbnails must always exist if corresponding preview exists")
                }
                return true
            }
            return false
        }
    }
}
