package space.taran.arknavigator.ui.fragments.preview

import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.extra.ImageMetaExtra
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.extension
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.notExists

data class PreviewAndThumbnail(val preview: Path, val thumbnail: Path) {

    companion object {

        private val PREVIEWS_STORAGE: Path =
            Paths.get("${App.instance.cacheDir}/previews")
        private val THUMBNAILS_STORAGE: Path =
            Paths.get("${App.instance.cacheDir}/thumbnails")

        private const val THUMBNAIL_WIDTH = 72
        private const val THUMBNAIL_HEIGHT = 128
        private const val COMPRESSION_QUALITY = 100

        private fun previewPath(id: ResourceId): Path =
            PREVIEWS_STORAGE.resolve(id.toString())
        private fun thumbnailPath(id: ResourceId): Path =
            THUMBNAILS_STORAGE.resolve(id.toString())

        init {
            if (PREVIEWS_STORAGE.notExists()) Files.createDirectories(PREVIEWS_STORAGE)
            if (THUMBNAILS_STORAGE.notExists()) Files.createDirectories(THUMBNAILS_STORAGE)
        }

        fun locate(path: Path, resource: ResourceMeta): PreviewAndThumbnail? {
            val thumbnail = thumbnailPath(resource.id)
            if (!Files.exists(thumbnail)) {
                //means that we couldn't generate anything for this kind of resource
                return null
            }

            if (ImageMetaExtra.ACCEPTED_EXTENSIONS.contains(extension(path))) {
                return PreviewAndThumbnail(
                    preview = path, //using the resource itself as its preview
                    thumbnail = thumbnail)
            }

            return PreviewAndThumbnail(
                preview = previewPath(resource.id),
                thumbnail = thumbnail)
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

            if (Files.exists(previewPath)) {
                if (!Files.exists(thumbnailPath)) {
                    throw AssertionError("Thumbnails must always exist if corresponding preview exists")
                }
                return
            }

            val ext = extension(path)

            if (ImageMetaExtra.ACCEPTED_EXTENSIONS.contains(ext)) {
                // images are special kind of a resource:
                // we don't need to store preview file for them,
                // we only need to downscale them into thumbnail

                val thumbnail: Bitmap = Glide.with(App.instance)
                        .asBitmap()
                        .load(path.toFile())
                        .apply(RequestOptions().override(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT))
                        .fitCenter()
                        .submit()
                        .get()


                storeThumbnail(thumbnailPath, thumbnail)
                return
            }

            val generator = PreviewGenerators.BY_EXT[ext]
            if (generator != null) {
                val preview = generator(path)

                val thumbnail: Bitmap = Glide.with(App.instance)
                        .asBitmap()
                        .load(preview)
                        .apply(RequestOptions().override(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT))
                        .fitCenter()
                        .submit()
                        .get()


                storePreview(previewPath, preview)
                storeThumbnail(thumbnailPath, thumbnail)

                return
            }
        }

        private fun storePreview(path: Path, bitmap: Bitmap) =
            storeImage(path, bitmap)

        private fun storeThumbnail(path: Path, bitmap: Bitmap) {
            if (bitmap.width > THUMBNAIL_WIDTH) {
                throw AssertionError("Bitmap must be downscaled")
            }
            if (bitmap.height > THUMBNAIL_HEIGHT) {
                throw AssertionError("Bitmap must be downscaled")
            }

            storeImage(path, bitmap)
        }

        private fun storeImage(target: Path, bitmap: Bitmap) {
            Files.newOutputStream(target).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, out)
                out.flush()
            }
        }
    }
}