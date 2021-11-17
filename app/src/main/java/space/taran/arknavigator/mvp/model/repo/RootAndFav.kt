package space.taran.arknavigator.mvp.model.repo

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.nio.file.Path
import kotlin.io.path.Path

@Parcelize
data class RootAndFav(private val rootString: String?, private val favString: String?): Parcelable {
    val root: Path? = rootString?.let { Path(it) }
    val fav: Path? = favString?.let { Path(it) }

    init {
        if (rootString == null && favString != null)
            throw AssertionError("Combination null root and not null fav isn't allowed")
    }

    fun isAllRoots(): Boolean {
        return rootString == null
    }
}