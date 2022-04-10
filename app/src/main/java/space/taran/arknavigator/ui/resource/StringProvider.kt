package space.taran.arknavigator.ui.resource

import android.content.Context
import androidx.annotation.StringRes
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.model.repo.index.ResourceKind

class StringProvider(private val context: Context) {
    fun getString(@StringRes stringResId: Int): String {
        return context.getString(stringResId)
    }

    fun kindToString(kind: ResourceKind) = when (kind) {
        ResourceKind.IMAGE -> context.getString(R.string.kind_image)
        ResourceKind.VIDEO -> context.getString(R.string.kind_video)
        ResourceKind.DOCUMENT -> context.getString(R.string.kind_document)
    }
}
