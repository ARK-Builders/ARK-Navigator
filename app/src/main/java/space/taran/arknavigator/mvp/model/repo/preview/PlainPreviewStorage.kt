package space.taran.arknavigator.mvp.model.repo.preview

import android.util.Log
import space.taran.arknavigator.mvp.model.arkFolder
import space.taran.arknavigator.mvp.model.arkPreviews
import space.taran.arknavigator.mvp.model.arkThumbnails
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.kind.ImageKindFactory
import space.taran.arknavigator.utils.LogTags.PREVIEWS
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory

class PlainPreviewStorage(val root: Path) : PreviewStorage {
    private val previewsDir = root.arkFolder().arkPreviews()
    private val thumbnailsDir = root.arkFolder().arkThumbnails()

    private fun previewPath(id: ResourceId): Path =
        previewsDir.resolve(id.toString())
    private fun thumbnailPath(id: ResourceId): Path =
        thumbnailsDir.resolve(id.toString())

    init {
        previewsDir.createDirectories()
        thumbnailsDir.createDirectories()
    }

    override fun locate(path: Path, resource: ResourceMeta): PreviewAndThumbnail? {
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

    override fun forget(id: ResourceId) {
        previewPath(id).deleteIfExists()
        thumbnailPath(id).deleteIfExists()
    }

    override fun store(path: Path, meta: ResourceMeta) {
        require(!path.isDirectory()) { "Previews for folders are constant" }

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
