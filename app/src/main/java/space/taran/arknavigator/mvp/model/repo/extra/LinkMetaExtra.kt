package space.taran.arknavigator.mvp.model.repo.extra

import com.beust.klaxon.Klaxon
import space.taran.arknavigator.mvp.model.repo.index.MetaExtraTag
import space.taran.arknavigator.mvp.model.repo.index.ResourceMetaExtra
import java.nio.file.Path
import java.util.zip.ZipFile

object LinkMetaExtra {
    val ACCEPTED_EXTENSIONS = setOf("link")
    private const val JSON_FILE = "link.json"
    val klaxon = Klaxon()

    fun extract(path: Path): ResourceMetaExtra? {
        val result = mutableMapOf<MetaExtraTag, String>()

        val zip = ZipFile(path.toFile())
        val jsonEntry = zip
            .entries()
            .asSequence()
            .find { entry -> entry.name == JSON_FILE }
            ?: return null

        val link = klaxon.parse<JsonLink>(zip.getInputStream(jsonEntry))
            ?: return null

        result[MetaExtraTag.TITLE] = link.title
        result[MetaExtraTag.DESCRIPTION] = link.desc
        result[MetaExtraTag.URL] = link.url

        return ResourceMetaExtra(result)
    }
}

private data class JsonLink(val url: String, val title: String, val desc: String)
