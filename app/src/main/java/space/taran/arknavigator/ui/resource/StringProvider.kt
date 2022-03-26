package space.taran.arknavigator.ui.resource

import android.content.Context
import androidx.annotation.StringRes
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.model.repo.kind.ResourceKind

class StringProvider(private val context: Context) {
    fun getString(@StringRes stringResId: Int): String {
        return context.getString(stringResId)
    }

    fun kindToString(kind: ResourceKind) = when (kind) {
        is ResourceKind.Image -> context.getString(R.string.kind_image)
        is ResourceKind.Video -> context.getString(R.string.kind_video)
        is ResourceKind.Document -> context.getString(R.string.kind_document)
        is ResourceKind.Link -> context.getString(R.string.kind_link)
    }
}
