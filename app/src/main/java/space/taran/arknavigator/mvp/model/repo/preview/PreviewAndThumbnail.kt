package space.taran.arknavigator.mvp.model.repo.preview

import android.util.Log
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.kind.ImageKindFactory
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.LogTags.PREVIEWS
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

        fun initDirs() {
            Files.createDirectories(PREVIEWS_STORAGE)
            Files.createDirectories(THUMBNAILS_STORAGE)
        }

        fun locate(path: Path, resource: ResourceMeta): PreviewAndThumbnail? {
            val thumbnail = thumbnailPath(resource.id)
            if (!Files.exists(thumbnail)) {
                Log.w(PREVIEWS, "thumbnail was not found for resource $resource")
                if (Files.exists(previewPath(resource.id))) {
                    throw AssertionError(
                        "Preview exists but thumbnail doesn't"
                    )
                }
                // means that we couldn't generate anything for this kind of resource
                return null
            }

            if (ImageKindFactory.isValid(path)) {
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

            if (!imagesExist(path, previewPath, thumbnailPath)) {
                Log.d(
                    PREVIEWS,
                    "Generating preview/thumbnail for ${meta.id} ($path)"
                )
                GeneralPreviewGenerator.generate(path, previewPath, thumbnailPath)
            }
        }

        private fun imagesExist(
            path: Path,
            previewPath: Path,
            thumbnailPath: Path
        ): Boolean {
            if (Files.exists(previewPath)) {
                if (!Files.exists(thumbnailPath)) {
                    throw AssertionError(
                        """Thumbnails must always exist
                            | if corresponding preview exists"""
                    )
                }
                return true
            }

            if (ImageKindFactory.isValid(path)) {
                return Files.exists(thumbnailPath)
            }

            return false
        }
    }
}
