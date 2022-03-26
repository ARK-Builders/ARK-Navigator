package space.taran.arknavigator.mvp.model.repo.kind

sealed class ResourceKind(val code: KindCode) {
    class Image : ResourceKind(KindCode.Image)

    class Video(
        val height: Long? = null,
        val width: Long? = null,
        val duration: Long? = null
    ) : ResourceKind(KindCode.Video)

    class Document(val pages: Int? = null) : ResourceKind(KindCode.Document)

    class Link(
        val title: String? = null,
        val description: String? = null,
        val url: String? = null
    ) : ResourceKind(KindCode.Link)
}

// These enums are only used to store different kind in one table in Room
enum class KindCode {
    Image, Video, Document, Link
}

enum class MetaExtraTag {
    DURATION, WIDTH, HEIGHT, PAGES, TITLE, DESCRIPTION, URL
}
